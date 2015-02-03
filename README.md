# lein-init-script

A leiningen plugin that generates *NIX init scripts.

In a nutshell, LIS generates the following artifacts which can be found in your <project-root>/init-script dir:

* Project Uberjar
<br />

* <your-project-name>d script

	Paired down from the Fedora Core init script template, injected with values from your lein project.clj.
<br />

* install-<your-project-name> script

	Creates (but does not overwrite) the :pid-dir, :install-dir, and :init-script-install-dir directories.  To override the defaults see the Configuration section below.
<br />

* clean-<your-project-name> script

	Removes the init script, and uberjar from their respective install paths.  Does not remove any created directories.
<br />

If you have an feature suggestions / bug reports, please open up an [issue](https://github.com/strongh/lein-init-script/issues)

## Why?

Because it was too damn time-consuming to turn a java program into a *nix daemon service that can be started / stopped asyncronously, chkconfig'd, etc.

## Setup

For Leiningen v1.x, add <code>[org.clojars.strongh/lein-init-script "1.3.1"]</code> to the :dev-dependencies section in your project.clj.

With Leiningen v2, add <code>[org.clojars.strongh/lein-init-script "1.3.1"]</code> to your user plugins.

## Configuration

lein-init-script takes several options in the form of:

	{:name "override-project-name"
     :pid-dir "/var/run"
     :install-dir "/usr/local/my-project-name"
     :init-script-install-dir "/etc/init.d"
	 :redirect-output-to "/log/file"
	 :properties {:clj-config.env "dev"
				  :java.library.path "/some/dir"
				  :init.script.test.prop "prop with spaces"}
     :jar-args ["-p" "8080"]
	 :jvm-opts ["-server"
				 "-Xms256M"
				 "-Xmx512M"
				 "-XX:MaxPermSize=128M"]}

which are passed to the the init-script task by adding a :lis-opts entry to the project map. For example:

	(defproject init-script-test "0.1.0"
	  :description "Test project for lein-init-script"
	  :dependencies [[org.clojure/clojure "1.1.0"]
	                 [org.clojure/clojure-contrib "1.1.0"]]
	  :dev-dependencies [[org.clojars.strongh/lein-init-script "1.3.1"]]
	  :lis-opts {:redirect-output-to "/var/log/init-script-test.log"
	             :properties {:clj-config.env "dev"
				  			  :java.library.path "/some/dir"
				  			  :init.script.test.prop "prop with spaces"}
		         :jvm-opts ["-server"
							 "-Xms256M"
				 			 "-Xmx512M"
				 			 "-XX:MaxPermSize=128M"]}
	  :main main)



## Usage

Create a main class for your project, run <code>lein init-script</code>, and check the ./init-script directory. Typically you'll want to run <code>./init-script/install-project</code> and then restart with e.g. <code>/etc/init.d/projectd restart</code>.

### ring uberjar and others

If you're using `ring uberjar` you can disable running vanilla `uberjar` by `init-script`. To do this set `:run-uberjar?` to false and run arbitrary `xxx uberjar` before calling init-script. To do it in one like use `do` task:

```bash
lein do ring uberjar, init-script
```

## Limitations

No Windows support at this time, if you'd like to see support for windows services, please open up an issue.

## License

[Eclipse Public License v1.0](http://www.eclipse.org/legal/epl-v10.html)
