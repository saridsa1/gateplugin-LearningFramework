@ECHO OFF
REM 001
SET ROOTDIR=%WRAPPER_HOME%
if "%ROOTDIR%"=="" (
  set ROOTDIR=%~dp0
)
SET data=%1
shift
SET model=%1
shift

: create var with remaining arguments
set r=%1
:loop
shift
if [%1]==[] goto done
set r=%r% %1
goto loop
:done

if [%PYTHON_BIN%]==[] goto nopython
%PYTHON_BIN% %ROOTDIR%\FileJsonPyTorch\gate-lf-pytorch-json\train.py %data% %model% %r%
goto exit
:nopython
python %ROOTDIR%\FileJsonPyTorch\gate-lf-pytorch-json\train.py %data% %model% %r%
:exit
