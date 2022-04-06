(ns heraldry.reader.blazonry.parser
  (:require
   [clojure.string :as s]
   [clojure.walk :as walk]
   [instaparse.combinators-source :as c]
   [instaparse.core :refer [parser]]
   [shadow.resource :as res]))

(defmacro load-grammar-template []
  (res/slurp-resource &env "grammar.ebnf"))

(defmacro load-default-grammar []
  (-> (res/slurp-resource &env "grammar.ebnf")
      (s/replace #"\{% charge-types %\}" "")))

(defmacro default-parser
  "Based on instaparse's defparser"
  []
  (let [macro-time-parser (parser (load-default-grammar))
        pre-processed-grammar (:grammar macro-time-parser)
        grammar-producing-code
        (->> pre-processed-grammar
             (walk/postwalk
              (fn [form]
                (cond
                  ;; Lists cannot be evaluated verbatim
                  (seq? form)
                  (list* 'list form)

                  ;; Regexp terminals are handled differently in cljs
                  (= :regexp (:tag form))
                  `(merge (c/regexp ~(str (:regexp form)))
                          ~(dissoc form :tag :regexp))

                  :else form))))]
    `(parser ~grammar-producing-code
             :start :blazon
             :auto-whitespace :standard)))
