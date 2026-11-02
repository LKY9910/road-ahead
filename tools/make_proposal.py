"""Fill the contest application form (DOCX) from docs/PROPOSAL.md section 3.

usage: py -3.12 -I tools/make_proposal.py <form.docx> <out.docx>
       py -3.12 -I tools/make_proposal.py --self-test

Each answer box keeps its content control (alias/tag/id) so the organizer can still
extract it by tag; the plain-text control becomes a rich block control holding
paragraphs, nested tables and images. Personal-info fields (01 name/contact, 04
signature) are left for the user. Text inside 【】 is highlighted as a to-do.
"""
import os
import re
import struct
import sys
import zipfile
from xml.sax.saxutils import escape

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PROPOSAL = os.path.join(ROOT, 'docs', 'PROPOSAL.md')
TAGS = ['app_summary', 'planning_background', 'vehicle_scenario', 'key_features',
        'pleos_plan', 'reference_files', 'reference_link', 'shortform_link']
BULLET_NUM_ID = 90
TEXT_WIDTH = 9600  # answer cell 9866 dxa minus 2 x 110 margins, rounded down
SZ, TABLE_SZ = 20, 19


# ---------- markdown -> blocks ----------

def proposal_sections(md):
    body = ('\n' + md).split('\n## 3.', 1)[1].split('\n## 4.', 1)
    parts = re.split(r'^### (\d)\. .*$', body[0], flags=re.M)
    sections = {int(parts[i]): parts[i + 1].strip('\n').split('\n') for i in range(1, len(parts), 2)}
    sources = [l for l in body[1].split('\n## 5.', 1)[0].split('\n') if l.startswith('- ')]
    return sections, sources


def blocks(lines):
    out, i = [], 0
    while i < len(lines):
        line = lines[i]
        if not line.strip() or line.startswith('>'):
            i += 1
        elif line.startswith('|'):
            rows = []
            while i < len(lines) and lines[i].startswith('|'):
                cells = [c.strip() for c in lines[i].strip().strip('|').split('|')]
                if not all(re.fullmatch(r':?-+:?', c) for c in cells):
                    rows.append(cells)
                i += 1
            out.append(('table', rows))
        elif line.startswith('!['):
            images = []
            while i < len(lines) and lines[i].startswith('!['):
                m = re.match(r'!\[([^\]]*)\]\(([^)]+)\)', lines[i])
                assert m, lines[i]
                images.append((m[1], m[2]))
                i += 1
            out.append(('images', images))
        else:
            m = re.match(r'^( *)(- |(\d+)\. )(.*)$', line)
            if m:
                level = len(m[1]) // 2
                out.append(('num', level, m[3], m[4]) if m[3] is not None else ('bullet', level, m[4]))
            elif re.match(r'^\*\*[^*]+\*\*', line):
                out.append(('heading', line))
            else:
                out.append(('para', line))
            i += 1
    return out


# ---------- inline -> runs ----------

def link_text(text, target):
    if target.startswith('http'):
        return f'{text} {target}' if text != target else target
    return text


def runs(text, size=SZ, bold=False, color=None):
    text = re.sub(r'\[([^\]]+)\]\(([^)]+)\)', lambda m: link_text(m[1], m[2]), text)
    out = []
    for token in re.split(r'(\*\*[^*]+\*\*|`[^`]+`|【[^】]*】)', text):
        if not token:
            continue
        b, hl = bold, False
        if token.startswith('**'):
            token, b = token[2:-2], True
        elif token.startswith('`'):
            token = token[1:-1]
        elif token.startswith('【'):
            hl = True
        props = ('<w:b/><w:bCs/>' if b else '') + (f'<w:color w:val="{color}"/>' if color else '') + \
            f'<w:sz w:val="{size}"/><w:szCs w:val="{size}"/>' + ('<w:highlight w:val="yellow"/>' if hl else '') + \
            '<w:lang w:eastAsia="ko-KR"/>'
        out.append(f'<w:r><w:rPr>{props}</w:rPr><w:t xml:space="preserve">{escape(token)}</w:t></w:r>')
    return ''.join(out)


def para(content, numpr='', ind=''):
    # pPr children follow the schema order: numPr, spacing, ind
    return f'<w:p><w:pPr>{numpr}<w:spacing w:before="0" w:after="60" w:line="288" w:lineRule="auto"/>{ind}</w:pPr>{content}</w:p>'


# ---------- blocks -> OOXML ----------

def col_widths(rows, total):
    longest = [max(len(re.sub(r'[*`]', '', r[c])) if c < len(r) else 0 for r in rows) for c in range(len(rows[0]))]
    weights = [min(max(n, 4), 60) for n in longest]
    widths = [max(int(total * w / sum(weights)), 1100) for w in weights]
    widths[-1] = total - sum(widths[:-1])
    return widths


def table_xml(rows, total=TEXT_WIDTH):
    widths = col_widths(rows, total)
    border = '<w:{0} w:val="single" w:sz="4" w:space="0" w:color="8394A5"/>'
    borders = ''.join(border.format(s) for s in ('top', 'left', 'bottom', 'right', 'insideH', 'insideV'))
    out = [f'<w:tbl><w:tblPr><w:tblW w:w="{total}" w:type="dxa"/><w:tblBorders>{borders}</w:tblBorders>'
           '<w:tblLayout w:type="fixed"/><w:tblCellMar><w:left w:w="80" w:type="dxa"/><w:right w:w="80" w:type="dxa"/></w:tblCellMar></w:tblPr>',
           '<w:tblGrid>' + ''.join(f'<w:gridCol w:w="{w}"/>' for w in widths) + '</w:tblGrid>']
    for r, row in enumerate(rows):
        out.append('<w:tr>' + ('<w:trPr><w:tblHeader/></w:trPr>' if r == 0 else ''))
        for c, w in enumerate(widths):
            shade = '<w:shd w:val="clear" w:color="auto" w:fill="E3E9F0"/>' if r == 0 else ''
            text = row[c] if c < len(row) else ''
            out.append(f'<w:tc><w:tcPr><w:tcW w:w="{w}" w:type="dxa"/>{shade}</w:tcPr>'
                       f'<w:p><w:pPr><w:spacing w:before="20" w:after="20" w:line="264" w:lineRule="auto"/></w:pPr>'
                       f'{runs(text, TABLE_SZ, bold=r == 0)}</w:p></w:tc>')
        out.append('</w:tr>')
    return ''.join(out) + '</w:tbl>'


def png_size(path):
    with open(path, 'rb') as f:
        head = f.read(24)
    assert head[:8] == b'\x89PNG\r\n\x1a\n', path
    return struct.unpack('>II', head[16:24])


class Media:
    def __init__(self):
        self.files = []  # (zip name, local path, rel id)

    def add(self, path):
        rid = f'rIdProposal{len(self.files) + 1}'
        self.files.append((f'media/proposal{len(self.files) + 1}.png', path, rid))
        return rid


def image_xml(media, path, width_dxa, n):
    w, h = png_size(path)
    cx = width_dxa * 635  # dxa -> EMU
    cy = cx * h // w
    rid = media.add(path)
    return (f'<w:r><w:drawing><wp:inline distT="0" distB="0" distL="0" distR="0"><wp:extent cx="{cx}" cy="{cy}"/>'
            f'<wp:docPr id="{9000 + n}" name="proposal image {n}"/>'
            '<a:graphic xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">'
            '<a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">'
            '<pic:pic xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">'
            f'<pic:nvPicPr><pic:cNvPr id="{9000 + n}" name="proposal{n}.png"/><pic:cNvPicPr/></pic:nvPicPr>'
            f'<pic:blipFill><a:blip r:embed="{rid}"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill>'
            f'<pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="{cx}" cy="{cy}"/></a:xfrm>'
            '<a:prstGeom prst="rect"><a:avLst/></a:prstGeom></pic:spPr></pic:pic></a:graphicData></a:graphic>'
            '</wp:inline></w:drawing></w:r>')


def images_xml(media, images, counter):
    per_row = 2 if len(images) > 1 else 1
    width = TEXT_WIDTH // per_row
    none = ''.join(f'<w:{s} w:val="nil"/>' for s in ('top', 'left', 'bottom', 'right', 'insideH', 'insideV'))
    out = [f'<w:tbl><w:tblPr><w:tblW w:w="{TEXT_WIDTH}" w:type="dxa"/><w:tblBorders>{none}</w:tblBorders>'
           '<w:tblLayout w:type="fixed"/></w:tblPr><w:tblGrid>' + f'<w:gridCol w:w="{width}"/>' * per_row + '</w:tblGrid>']
    for start in range(0, len(images), per_row):
        out.append('<w:tr><w:trPr><w:cantSplit/></w:trPr>')
        for caption, rel in (images[start:start + per_row] + [('', '')] * per_row)[:per_row]:
            cell = ''
            if rel:
                counter[0] += 1
                path = os.path.normpath(os.path.join(ROOT, 'docs', rel))
                cell = (f'<w:p><w:pPr><w:spacing w:before="40" w:after="0"/><w:jc w:val="center"/></w:pPr>'
                        f'{image_xml(media, path, width - 280, counter[0])}</w:p>'
                        f'<w:p><w:pPr><w:spacing w:before="20" w:after="80"/><w:jc w:val="center"/></w:pPr>'
                        f'{runs(caption, TABLE_SZ, color="44546A")}</w:p>')
            out.append(f'<w:tc><w:tcPr><w:tcW w:w="{width}" w:type="dxa"/></w:tcPr>{cell or "<w:p/>"}</w:tc>')
        out.append('</w:tr>')
    return ''.join(out) + '</w:tbl>'


def blocks_xml(items, media, counter):
    out = []
    for item in items:
        kind = item[0]
        if kind == 'para':
            out.append(para(runs(item[1])))
        elif kind == 'heading':
            out.append(f'<w:p><w:pPr><w:keepNext/><w:spacing w:before="160" w:after="60" w:line="288" w:lineRule="auto"/></w:pPr>'
                       f'{runs(item[1], color="16324F")}</w:p>')
        elif kind == 'bullet':
            out.append(para(runs(item[2]), f'<w:numPr><w:ilvl w:val="{min(item[1], 2)}"/><w:numId w:val="{BULLET_NUM_ID}"/></w:numPr>'))
        elif kind == 'num':
            left = 400 * (item[1] + 1)
            out.append(para(runs(f'{item[2]}. ') + runs(item[3]), ind=f'<w:ind w:left="{left}" w:hanging="320"/>'))
        elif kind == 'table':
            out.append(table_xml(item[1]))
        elif kind == 'images':
            out.append(images_xml(media, item[1], counter))
    if not out or out[-1].startswith('<w:tbl>'):
        out.append('<w:p><w:pPr><w:spacing w:after="0"/></w:pPr></w:p>')
    return ''.join(out)


def source_lines(sources):
    items: list[tuple] = [('heading', '**근거 출처**')]
    for line in sources:
        text = re.sub(r'\[([^\]]+)\]\(([^)]+)\)',
                      lambda m: link_text(m[1], m[2]) if m[2].startswith('http') else f'저장소 docs/{m[2]}', line[2:])
        items.append(('bullet', 0, text.replace('*', '')))
    return items


# ---------- form editing ----------

BULLET_ABSTRACT = (
    '<w:abstractNum w:abstractNumId="90"><w:multiLevelType w:val="hybridMultilevel"/>'
    + ''.join(f'<w:lvl w:ilvl="{i}"><w:start w:val="1"/><w:numFmt w:val="bullet"/><w:lvlText w:val="{c}"/>'
              f'<w:lvlJc w:val="left"/><w:pPr><w:ind w:left="{360 + 360 * i}" w:hanging="240"/></w:pPr>'
              f'<w:rPr><w:rFonts w:ascii="맑은 고딕" w:eastAsia="맑은 고딕" w:hAnsi="맑은 고딕" w:hint="eastAsia"/></w:rPr></w:lvl>'
              for i, c in enumerate(['•', '–', '·']))
    + '</w:abstractNum>')


def fill_box(doc, tag, inner):
    m = re.search(r'<w:p\b(?:(?!<w:p\b).)*?<w:sdt><w:sdtPr>((?:(?!</w:sdtPr>).)*?<w:tag w:val="'
                  + tag + r'"/>.*?)</w:sdtPr>.*?</w:sdt></w:p>', doc, re.S)
    assert m, tag
    props = re.sub(r'<w:text\b[^>]*/>', '', m[1])
    block = (f'<w:sdt><w:sdtPr>{props}</w:sdtPr><w:sdtContent>{inner}</w:sdtContent></w:sdt>'
             '<w:p><w:pPr><w:spacing w:after="0" w:line="20" w:lineRule="exact"/></w:pPr></w:p>')
    # The box row is cantSplit; a filled box is taller than a page, so let it flow.
    row = doc.rindex('<w:tr ', 0, m.start())
    head = doc[row:m.start()].replace('<w:cantSplit/>', '', 1)
    return doc[:row] + head + block + doc[m.end():]


def para_text(p):
    return ''.join(re.findall(r'<w:t(?: [^>]*)?>([^<]*)</w:t>', p))


def set_paragraph_text(doc, startswith, text, count):
    paras = [m for m in re.finditer(r'<w:p\b[^>]*>(?:(?!</w:p>).)*</w:p>', doc, re.S)
             if para_text(m[0]).replace(' ', '').startswith(startswith.replace(' ', ''))]
    assert len(paras) == count, (startswith, len(paras))
    for m in reversed(paras):
        p = m[0]
        ppr = re.search(r'<w:pPr>.*?</w:pPr>', p, re.S)
        rpr = re.search(r'<w:r\b[^>]*>(<w:rPr>.*?</w:rPr>)', p, re.S)
        new = (p[:p.index('>') + 1] + (ppr[0] if ppr else '') +
               f'<w:r>{rpr[1] if rpr else ""}<w:t xml:space="preserve">{escape(text)}</w:t></w:r></w:p>')
        doc = doc[:m.start()] + new + doc[m.end():]
    return doc


def build(form, out_path, md):
    sections, sources = proposal_sections(md)
    media, counter = Media(), [0]
    zin = zipfile.ZipFile(form)
    doc = zin.read('word/document.xml').decode('utf-8')
    for n, tag in enumerate(TAGS, start=1):
        if n == 8:
            continue  # short-form link: optional, the user decides
        items = blocks(sections[n])
        if n == 7:
            items += source_lines(sources)
        doc = fill_box(doc, tag, blocks_xml(items, media, counter))
    doc = set_paragraph_text(doc, '참가 단위:', '참가 단위: ■ 개인           □ 팀', 1)
    doc = set_paragraph_text(doc, '참가 부문:', '참가 부문: ■ 대학(원)생    □ 일반(스타트업 포함)', 1)
    doc = set_paragraph_text(doc, '앱명 / 프로젝트명:', '앱명 / 프로젝트명: 내비서', 3)
    doc = set_paragraph_text(doc, '참가 인원:', '참가 인원: 1명', 1)

    numbering = zin.read('word/numbering.xml').decode('utf-8')
    first_num = numbering.index('<w:num ')
    numbering = (numbering[:first_num] + BULLET_ABSTRACT + numbering[first_num:]).replace(
        '</w:numbering>', f'<w:num w:numId="{BULLET_NUM_ID}"><w:abstractNumId w:val="90"/></w:num></w:numbering>')
    rels = zin.read('word/_rels/document.xml.rels').decode('utf-8').replace('</Relationships>', ''.join(
        f'<Relationship Id="{rid}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="{name}"/>'
        for name, _, rid in media.files) + '</Relationships>')
    types = zin.read('[Content_Types].xml').decode('utf-8')
    if media.files and 'Extension="png"' not in types:
        types = types.replace('<Default ', '<Default Extension="png" ContentType="image/png"/><Default ', 1)

    replaced = {'word/document.xml': doc, 'word/numbering.xml': numbering,
                'word/_rels/document.xml.rels': rels, '[Content_Types].xml': types}
    with zipfile.ZipFile(out_path, 'w', zipfile.ZIP_DEFLATED) as zout:
        for info in zin.infolist():
            data = replaced.get(info.filename)
            zout.writestr(info, data.encode('utf-8') if data is not None else zin.read(info.filename))
        for name, path, _ in media.files:
            zout.write(path, 'word/' + name)
    return counter[0]


def self_test():
    md = ('## 3. x\n### 1. a\n**굵게** (메모)\n- 하나 `코드`\n  - 둘\n  1. 셋\n\n| 가 | 나 |\n|---|---|\n| 1 | [링크](https://e.x) |\n'
          '### 8. b\n텍스트 【기입】\n## 4. 출처\n- 근거: [기사](https://n.x), [실측](spikes/a.md)\n## 5. 끝\n')
    sections, sources = proposal_sections(md)
    kinds = [b[0] for b in blocks(sections[1])]
    assert kinds == ['heading', 'bullet', 'bullet', 'num', 'table'], kinds
    assert blocks(sections[1])[2][1] == 1 and blocks(sections[1])[3][1:3] == (1, '1')
    xml = blocks_xml(blocks(sections[1]), Media(), [0])
    assert '<w:b/>' in xml and '링크 https://e.x' in xml and xml.endswith('</w:p>')
    assert '<w:highlight w:val="yellow"/>' in runs('텍스트 【기입】')
    assert '저장소 docs/spikes/a.md' in blocks_xml(source_lines(sources), Media(), [0])
    assert col_widths([['a', 'bbbbbbbb'], ['c', 'd']], 9600)[0] >= 1100
    doc = ('<w:tr w:rsidR="1"><w:trPr><w:cantSplit/></w:trPr><w:tc><w:p><w:pPr/><w:sdt><w:sdtPr><w:alias w:val="x"/>'
           '<w:tag w:val="app_summary"/><w:text/></w:sdtPr><w:sdtContent><w:r><w:t> </w:t></w:r></w:sdtContent></w:sdt></w:p></w:tc></w:tr>')
    filled = fill_box(doc, 'app_summary', '<w:p/>')
    assert '<w:text' not in filled and 'w:tag w:val="app_summary"' in filled and filled.count('<w:sdt>') == 1
    assert '<w:cantSplit/>' not in filled and filled.endswith('</w:tc></w:tr>')
    print('self-test ok')


if __name__ == '__main__':
    if sys.argv[1:] == ['--self-test']:
        self_test()
    else:
        n = build(sys.argv[1], sys.argv[2], open(PROPOSAL, encoding='utf-8').read())
        print(f'wrote {sys.argv[2]} ({n} images)')
