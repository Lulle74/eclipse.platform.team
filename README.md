# eclipse.platform.team
Team project repository (eclipse.platform.team)
The rationale for this fork (of eclipse.platform.team) is to bring the bundle org.eclipse.compare up to something that can be called "minimum e4 compability". 

Background and links:
https://www.eclipse.org/forums/index.php/t/1087233/ (EMF Compare dialog in standalone RCP application e4 discussion)
https://bugs.eclipse.org/bugs/show_bug.cgi?id=473847 (The bug, as of March 2018 not resolved. Link to Gerrit patches are found here)

What has been done:
* Applied the Gerrit patches (affects org.eclipse.compare) 
* ...

Why do this?
To make the bundle org.eclipse.compare run within a pure E4 application. Most importantly, in such an application, the Workbench (traditionally 
accessed via PlatformUI.getWorkbench()) is simply not running. An exception is thrown from the access method, which makes 
it even more important to safe-guard such calls when running on E4. There are also a couple of other incompatible things that have been bridged. 
See the Gerrit patches for more information. 



 
