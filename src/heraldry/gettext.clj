(ns heraldry.gettext
  (:require [clojure.java.io :as io]
            [pottery.core :as pottery]
            [pottery.scan :as pottery-scan]
            [shadow.resource :as res]))

(defmacro inline-dict [filename]
  (pottery/read-po-str (res/slurp-resource &env filename)))

(defn gettext-do-scan! []
  (pottery/scan-codebase!
   {:dir "src"
    :template-file (io/file "gettext/template.pot")
    :extract-fn (pottery/make-extractor
                 ['string (s :guard string?)] s
                 [(:or 'string) & _] (pottery-scan/extraction-warning
                                      "Could not extract strings for the form:"))}))

(comment
  (gettext-do-scan!)

  ;;
  )
