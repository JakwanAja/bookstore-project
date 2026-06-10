"""
Fase 2 No. 5 - Script cek JaCoCo coverage.
Usage: python3 check_coverage.py <jacoco.csv> <threshold_pct>
Exit 0 = LULUS, Exit 1 = GAGAL (pipeline merah)
"""
import csv, sys

csv_path  = sys.argv[1] if len(sys.argv) > 1 else "target/site/jacoco/jacoco.csv"
threshold = float(sys.argv[2]) if len(sys.argv) > 2 else 75.0

total_missed   = 0
total_covered  = 0
branch_missed  = 0
branch_covered = 0

with open(csv_path) as f:
    reader = csv.DictReader(f)
    for row in reader:
        total_missed   += int(row.get("INSTRUCTION_MISSED", 0))
        total_covered  += int(row.get("INSTRUCTION_COVERED", 0))
        branch_missed  += int(row.get("BRANCH_MISSED", 0))
        branch_covered += int(row.get("BRANCH_COVERED", 0))

total_instr  = total_missed + total_covered
total_branch = branch_missed + branch_covered

stmt_pct   = (total_covered  / total_instr  * 100) if total_instr  > 0 else 0
branch_pct = (branch_covered / total_branch * 100) if total_branch > 0 else 0

print(f"============================================")
print(f"  LAPORAN JACOCO COVERAGE")
print(f"============================================")
print(f"  Statement Coverage : {stmt_pct:.1f}%  ({total_covered}/{total_instr} instruksi)")
print(f"  Branch Coverage    : {branch_pct:.1f}%  ({branch_covered}/{total_branch} branch)")
print(f"  Threshold          : {threshold}%")
print(f"============================================")

failed = False
if stmt_pct < threshold:
    print(f"  GAGAL: Statement Coverage {stmt_pct:.1f}% < {threshold}%")
    failed = True
else:
    print(f"  LULUS: Statement Coverage {stmt_pct:.1f}% >= {threshold}%")

if branch_pct < threshold:
    print(f"  GAGAL: Branch Coverage {branch_pct:.1f}% < {threshold}%")
    failed = True
else:
    print(f"  LULUS: Branch Coverage {branch_pct:.1f}% >= {threshold}%")

print(f"============================================")
sys.exit(1 if failed else 0)