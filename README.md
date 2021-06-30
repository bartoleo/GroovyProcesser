# GroovyProcesser

[![Join the chat at https://gitter.im/bartoleo/GroovyProcesser](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/bartoleo/GroovyProcesser?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Real time Groovy evaluation to process input data

Directives you can use in input panel (top):
- @gp:json
- @gp:xml
- @gp:url
- @gp:file
- @gp:groovy
- @gp:cmd 

@gp:json
--------
It parses the text below the directive as json and returns the value as input 

@gp:xml
--------
It parses the text below the directive as xml and returns the value as input 

@gp:url
--------
It fetches the text below the directive as an url and returns the content as input

@gp:file
--------
It fetches the text below the directive as an absolute path and returns the content of the file as input

@gp:groovy
--------
It executes the text below the directive as a groovy script and returns the returned value of the execution as input

@gp:cmd
--------
It executes the text below the directive as a shell and returns the stdout of the execution as input


Processer methods
-----------------
You can use these methods to create code templates:
- processer.capitalize(<string>)
- processer.decapitalize(<string>)
- processer.toPropertyName(<string>)
- processer.toGetter(<string>)
- processer.toSetter(<string>,<value>)
- processer.toCamelCase(<string>)
- processer.toSnakeCase(<string>)
- processer.toSausageCase(<string>)
- processor.rpad(<string>, <length>, <character_filler>)
- processor.lpad(<string>, <length>, <character_filler>)

You can also use the 'gp' alias instead of 'processer'
