(ns leiningen.init-script
  (:use [leiningen.uberjar])
  (:require [clojure.java.io :as io]))


(defn format-properties [opts]
  (if (nil? (:properties opts))
    ""
    (apply str (interpose " " (map #(str "-D" (name %) "=\"" 
					  (% (:properties opts)) "\"")
				   (keys (:properties opts)))))))

(defn format-jvm-opts [opts]
  (let [jvm-opts (:jvm-opts opts)]
    (apply str (interpose " " jvm-opts))))

(defn format-java-string [opts]
  (str (format-properties opts) " " (format-jvm-opts opts)))

(def gen-init-script)
(def gen-install-script)

(defn resource-input-stream [res-name]
  (.getResourceAsStream (.getContextClassLoader (Thread/currentThread)) res-name))

(def init-script-template (slurp (resource-input-stream "init-script-template")))
(def install-template (slurp (resource-input-stream "install-template")))
(def clean-template (slurp (resource-input-stream "clean-template")))

(defn gen-init-script [project opts]
  (let [name (:name project)
	version (:version project)
	description (:description project)
	pid-dir (:pid-dir opts)
	jar-install-dir (:jar-install-dir opts)
	java-flags (format-java-string opts)
	redirect-output-to (:redirect-output-to opts)]
    (format init-script-template name version pid-dir jar-install-dir java-flags redirect-output-to)))

(defn gen-install-script [uberjar-path init-script-path opts]
  (let [jar-install-dir (:jar-install-dir opts)
	init-script-install-dir (:init-script-install-dir opts)
	name (:name opts)
        version (:version opts)
	installed-init-script-path (str init-script-install-dir "/" name "d")]
    (format install-template 
	    name
            version
	    jar-install-dir 
	    uberjar-path 
	    init-script-install-dir 
	    init-script-path 
	    installed-init-script-path)))

(defn gen-clean-script [project opts]
  (let [jar-install-dir (:jar-install-dir opts)
	init-script-install-dir (:init-script-install-dir opts)
	name (:name project)]
    (format clean-template name jar-install-dir init-script-install-dir)))

(defn create-output-dir [path]
  (.mkdirs (java.io.File. path)))

(defn create-script [path content]
  (do (spit path content)
      (doto
          (java.io.File. path)
        (.setExecutable true))))

(defn defaults [project]
  (let [name (:name project)
	root (:root project)
	version (:version project)]
    {:name name
     :pid-dir "/var/run"
     :jar-install-dir (str "/usr/local/" name)
     :init-script-install-dir "/etc/init.d"
     :artifact-dir (str root "/init-script")
     :redirect-output-to "/dev/null"
     :version version}))

(defn init-script
  "A leiningen plugin that allows you to generate *NIX init scripts."
  [project]
  (let [opts (merge (defaults project) (:lis-opts project))
	root (:root project)
	name (:name opts)
	version (:version opts)
	artifact-dir (:artifact-dir opts)
	source-uberjar-path (str root "/target/" name "-" version "-standalone.jar")
	artifact-uberjar-path (str artifact-dir "/" name "-" version "-standalone.jar")
	artifact-init-script-path (str artifact-dir "/" name "d")
	install-script-path (str artifact-dir "/" "install-" name)
	clean-script-path (str artifact-dir "/" "clean-" name)]
    (create-output-dir artifact-dir)
    (uberjar project)
    (io/copy (java.io.File. source-uberjar-path) (java.io.File. artifact-uberjar-path))
    (create-script
     artifact-init-script-path (gen-init-script project opts))
    (create-script
     install-script-path 
     (gen-install-script artifact-uberjar-path artifact-init-script-path opts))
    (create-script
     clean-script-path (gen-clean-script project opts))
    (println (str "*** Done generating init scripts, see the " artifact-dir " directory"))))
