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
  compile.from javacc(_('src/main/javacc'), :in_package=>'com.idioticdev.javagrande')
  run.using :main => "com.idioticdev.javagrande.JavaGrande"
  package(:jar)
end