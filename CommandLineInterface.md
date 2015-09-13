The SRL editor can be used from the command line using the following syntax

```
java -cp SRLGUI.jar srl.project.Run -p project_directory [-i input_file] [-o output_file] [-n] [-e encoding]
```

  * `-p`: This should be the directory for the project. All rules and word lists will be loaded from this project, but the corpus is not used
  * `-i`: The input file. If this is not specified STDIN is used instead
  * `-o`: The output file. If this is not specified STDOUT is used instead
  * `-n`: If this is used then all entity rules will be applied to the input file and the tagged document will be outputted. If this is not used the template rule will be applied as well and the template extractions outputted
  * `-e`: Specify the encoding of the input files if this is not the system default. For example most European windows system use "windows-1252" for encoding however Linux systems use "UTF-8". Please check encoding if the extraction system is not working correctly.
