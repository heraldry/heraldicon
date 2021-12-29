(ns heraldry.gettext
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [pottery.core :as pottery]
            [pottery.scan :as pottery-scan]
            [shadow.resource :as res]))

(defmacro inline-dict [filename]
  (pottery/read-po-str (res/slurp-resource &env filename)))

(defn build-keyword-keys [data & {:keys [prefix]}]
  (->> data
       (mapcat (fn [[k v]]
                 (if (map? v)
                   (build-keyword-keys v :prefix (str prefix "." k))
                   [[(keyword prefix k) v]])))
       (into {})))

(defmacro inline-dict-json [filename]
  (let [data (json/read-str (res/slurp-resource &env filename))]
    (build-keyword-keys data :prefix "strings")))

(defn gettext-do-scan! []
  (pottery/scan-codebase!
   {:dir "src"
    :template-file (io/file "gettext/template.po")
    :extract-fn (pottery/make-extractor
                 ['string (s :guard string?)] s
                 [(:or 'string) & _] (pottery-scan/extraction-warning
                                      "Could not extract strings for the form:"))}))

(comment
  (gettext-do-scan!)

  ;;
  )
