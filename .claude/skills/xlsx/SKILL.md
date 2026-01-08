---
name: xlsx
description: Excel spreadsheet (.xlsx) creation, editing, and analysis with formulas, formatting, and data visualization. Use when working with Excel files.
---

# XLSX Processing

## Libraries

| Library | Purpose |
|---------|---------|
| pandas | Data analysis, read/write |
| openpyxl | Formulas, formatting, charts |
| xlsxwriter | Write-only, fast creation |

## Read Excel
```python
import pandas as pd

# Simple read
df = pd.read_excel("file.xlsx")

# Specific sheet
df = pd.read_excel("file.xlsx", sheet_name="Data")

# Multiple sheets
sheets = pd.read_excel("file.xlsx", sheet_name=None)
```

## Write Excel
```python
df.to_excel("output.xlsx", index=False)

# Multiple sheets
with pd.ExcelWriter("output.xlsx") as writer:
    df1.to_excel(writer, sheet_name="Sheet1")
    df2.to_excel(writer, sheet_name="Sheet2")
```

## Formulas (openpyxl)
```python
from openpyxl import Workbook

wb = Workbook()
ws = wb.active

ws["A1"] = 100
ws["A2"] = 200
ws["A3"] = "=SUM(A1:A2)"  # Formula, not hardcoded!
ws["B1"] = "=IF(A1>50,\"High\",\"Low\")"

wb.save("formulas.xlsx")
```

## Formatting
```python
from openpyxl.styles import Font, Fill, Alignment, Border

# Font
ws["A1"].font = Font(bold=True, size=14, color="0000FF")

# Alignment
ws["A1"].alignment = Alignment(horizontal="center")

# Number format
ws["B1"].number_format = "$#,##0.00"
ws["C1"].number_format = "0.0%"
```

## Financial Model Standards

### Color Coding
| Color | Meaning |
|-------|---------|
| Blue text | User inputs |
| Black text | Formulas |
| Green text | Worksheet links |
| Red text | External links |
| Yellow bg | Key assumptions |

### Number Formats
| Type | Format |
|------|--------|
| Currency | `$#,##0` |
| Percentage | `0.0%` |
| Zeros | `-` (dash) |
| Negatives | `(#,##0)` |

## Zero Errors Policy
Must eliminate: `#REF!`, `#DIV/0!`, `#VALUE!`, `#N/A`, `#NAME?`

```python
# Safe division
ws["A1"] = "=IF(B1=0,0,A1/B1)"

# Safe lookup
ws["A1"] = "=IFERROR(VLOOKUP(A1,Data,2,FALSE),\"\")"
```

## Charts
```python
from openpyxl.chart import BarChart, Reference

chart = BarChart()
data = Reference(ws, min_col=2, min_row=1, max_col=3, max_row=10)
chart.add_data(data, titles_from_data=True)
ws.add_chart(chart, "E5")
```

## Data Validation
```python
from openpyxl.worksheet.datavalidation import DataValidation

dv = DataValidation(type="list", formula1='"Yes,No,Maybe"')
ws.add_data_validation(dv)
dv.add("A1:A100")
```

## Dependencies
```bash
pip install pandas openpyxl xlsxwriter
```

## Critical Rules

| Rule | Reason |
|------|--------|
| Use formulas, not hardcoded | Maintains spreadsheet dynamism |
| Cell references, not values | Enables recalculation |
| Document assumptions | Source: company filings, etc. |
| Match existing format | When editing templates |
