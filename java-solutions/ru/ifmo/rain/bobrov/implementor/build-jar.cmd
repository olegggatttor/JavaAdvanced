cd ..\..\..\..\..\..\..\

SET root=%cd%
SET solutions=%root%\java-advanced-2020-solutions
SET utils=info\kgeorgiy\java\advanced\implementor
SET my_pack=ru\ifmo\rain\bobrov\implementor
SET modules_files=%root%\java-advanced-2020\modules\info.kgeorgiy.java.advanced.implementor\%utils%
SET out=%solutions%\_build\production
rmdir %out%
md %out%
md %out%\%my_pack%
md %out%\%utils%

copy %solutions%\java-solutions\%my_pack%\JarImplementor.java %out%\%my_pack%
copy %modules_files%\Impler.java %out%\%utils%
copy %modules_files%\JarImpler.java %out%\%utils%
copy %modules_files%\ImplerException.java %out%\%utils%

javac -cp %solutions%\_build\production -d %out% %out%\%my_pack%\JarImplementor.java %out%\%utils%\Impler.java %out%\%utils%\JarImpler.java %out%\%utils%\ImplerException.java
jar -c --file=%solutions%\java-solutions\%my_pack%\_implementor.jar --main-class=JarImplementor %out%\%my_pack%\JarImplementor.java %out%\%utils%\Impler.java %out%\%utils%\JarImpler.java %out%\%utils%\ImplerException.java
pause