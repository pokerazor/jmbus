::BATCH file for windows
set BATDIR=%~dp0
set LIBDIR=%BATDIR%..\cli-app\build\libs-all

java -Djava.ext.dirs=%LIBDIR% org.openmuc.jmbus.app.JmbusApp %*
