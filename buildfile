require "buildr/javacc"

# Version number for this release
VERSION_NUMBER = "0.0.1"
# Group identifier for your projects
GROUP = "JavaGrande"
COPYRIGHT = "Idiotic Design and Development"

# Specify Maven 2.0 remote repositories here, like this:
repositories.remote << "http://repo1.maven.org/maven2"

desc "The Javagrande project"
define "JavaGrande" do

  project.version = VERSION_NUMBER
  project.group = GROUP
  manifest["Implementation-Vendor"] = COPYRIGHT
  compile.with 'com.github.javaparser:javaparser-core:jar:2.1.0'
  compile.with 'com.lexicalscope.jewelcli:jewelcli:jar:0.8.9'
  compile.with 'org.hamcrest:hamcrest-all:jar:1.3'
  compile.from javacc(_('src/main/javacc'), :in_package=>'com.idioticdev.javagrande')
  run.using :main => ["com.idioticdev.javagrande.JavaGrande", "Test.java"]
  package(:jar)
  package(:jar).enhance { |pkg| pkg.enhance { |pkg| add_dependencies(pkg) }}
end

def add_dependencies(pkg)
  tempfile = pkg.to_s.sub(/.jar$/, "-without-dependencies.jar")
  mv pkg.to_s, tempfile

  dependencies = compile.dependencies.map { |d| "-c #{d}"}.join(" ")
  sh "java -jar tools/autojar.jar -bae -m manifest -o #{pkg} #{dependencies} #{tempfile} -C resources com/*"
end