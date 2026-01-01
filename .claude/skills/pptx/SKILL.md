---
name: pptx
description: PowerPoint presentation (.pptx) creation, editing, and analysis. Use when working with presentations.
---

# PPTX Processing

## Workflows

| Task | Method |
|------|--------|
| Read text | markitdown or pandoc |
| Create new | python-pptx |
| Edit existing | Unpack XML, modify, repack |
| HTML → PPTX | html2pptx conversion |

## Read Content
```bash
# Via pandoc
pandoc presentation.pptx -t markdown

# Via markitdown
markitdown presentation.pptx
```

```python
from pptx import Presentation

prs = Presentation("file.pptx")
for slide in prs.slides:
    for shape in slide.shapes:
        if hasattr(shape, "text"):
            print(shape.text)
```

## Create Presentation
```python
from pptx import Presentation
from pptx.util import Inches, Pt

prs = Presentation()

# Title slide
slide = prs.slides.add_slide(prs.slide_layouts[0])
slide.shapes.title.text = "Presentation Title"
slide.placeholders[1].text = "Subtitle"

# Content slide
slide = prs.slides.add_slide(prs.slide_layouts[1])
slide.shapes.title.text = "Slide Title"
body = slide.placeholders[1]
body.text = "First bullet"
p = body.text_frame.add_paragraph()
p.text = "Second bullet"
p.level = 1  # Indented

prs.save("output.pptx")
```

## Add Images
```python
slide.shapes.add_picture(
    "image.png",
    left=Inches(1),
    top=Inches(2),
    width=Inches(4)
)
```

## Add Tables
```python
table = slide.shapes.add_table(
    rows=3, cols=4,
    left=Inches(1), top=Inches(2),
    width=Inches(8), height=Inches(2)
).table

table.cell(0, 0).text = "Header 1"
```

## Add Charts
```python
from pptx.chart.data import CategoryChartData
from pptx.enum.chart import XL_CHART_TYPE

chart_data = CategoryChartData()
chart_data.categories = ["Q1", "Q2", "Q3"]
chart_data.add_series("Sales", (100, 150, 200))

slide.shapes.add_chart(
    XL_CHART_TYPE.COLUMN_CLUSTERED,
    Inches(1), Inches(2),
    Inches(8), Inches(4),
    chart_data
)
```

## Styling
```python
from pptx.dml.color import RgbColor
from pptx.enum.text import PP_ALIGN

# Font
run = para.runs[0]
run.font.bold = True
run.font.size = Pt(24)
run.font.color.rgb = RgbColor(0x00, 0x66, 0xCC)

# Alignment
para.alignment = PP_ALIGN.CENTER
```

## Edit via XML
```bash
# .pptx is a ZIP
unzip -d unpacked presentation.pptx

# Key files:
# - ppt/slides/slide1.xml (content)
# - ppt/slideLayouts/ (layouts)
# - ppt/slideMasters/ (master styles)

# After editing
zip -r modified.pptx unpacked/*
```

## Template Usage
```python
# Load template
prs = Presentation("template.pptx")

# Duplicate slide
slide_layout = prs.slide_layouts[1]
new_slide = prs.slides.add_slide(slide_layout)

# Replace placeholders
for shape in new_slide.placeholders:
    if shape.has_text_frame:
        shape.text = "New content"
```

## Dependencies
```bash
pip install python-pptx
npm install markitdown  # Optional for extraction
```

## Design Tips

| Principle | Guideline |
|-----------|-----------|
| Typography | Web-safe fonts, strong hierarchy |
| Layout | Two-column or full-slide |
| Colors | Cohesive palette, 3-5 colors max |
| Content | 6 bullets max, minimal text |
| Images | High resolution, consistent style |
