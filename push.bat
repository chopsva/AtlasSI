@echo off
IF "%~1"=="" (
	echo MUST PASS REASON/DESCR 
	exit /b 1
)

@echo on
git add .
git commit -m "%~1"
git push origin main
   