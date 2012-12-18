(ns leiningen.init-script-test
  (:use clojure.test
        leiningen.init-script))
;; Testing options
(def test-opts {:properties {:clj-config.env "dev"
			     :java.library.path "/some/dir"}
		:jvm-opts ["-server" 
			    "-Xms1G"
			    "-Xmx2G"
			    "-XX:MaxPermSize=128M"]})



(deftest init-script-tests
  (testing "format-properties"
    (is (= (format-properties {:properties {:foo "bar"}})  "-Dfoo=\"bar\"")))
  (testing "format-java-string"
    (is (= (format-java-string test-opts) "-Djava.library.path=\"/some/dir\" -Dclj-config.env=\"dev\" -server -Xms1G -Xmx2G -XX:MaxPermSize=128M")))
  )
