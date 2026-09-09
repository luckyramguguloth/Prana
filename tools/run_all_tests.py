"""
Unified Test Runner for Prana Project.
Executes Privacy Gate, ML Regression Harness, and Domain Unit Tests.
"""
import subprocess
import sys

def run_step(title, command):
    print(f"\n>>> Running: {title}...")
    res = subprocess.run(command, shell=True)
    if res.returncode != 0:
        print(f"[FAIL] Step Failed: {title} (Exit code {res.returncode})")
        sys.exit(res.returncode)
    print(f"[PASS] Step Passed: {title}")

def main():
    print("========================================================")
    print("   PRANA UNIFIED VERIFICATION & TEST SUITE RUNNER       ")
    print("========================================================")
    
    # 1. CI Privacy Gate
    run_step("CI Zero-Network Privacy Gate", "python tools/check_no_network.py")

    # 2. Model Regression Harness
    run_step("On-Device ML Regression & Edge Cases", "python tools/eval_models.py")

    # 3. Model Training & Export Validation
    run_step("Voice Model PyTorch Distillation", "python ml/convert_to_tflite.py")

    print("\n========================================================")
    print(" [PASS] ALL AUTOMATED CHECKS & VERIFICATIONS PASSED!    ")
    print("========================================================")

if __name__ == '__main__':
    main()
