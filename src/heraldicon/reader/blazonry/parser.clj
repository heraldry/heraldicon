(ns heraldicon.reader.blazonry.parser
  (:require
   [clojure.walk :as walk]
   [instaparse.combinators-source :as c]
   [instaparse.core :refer [parser]]
   [shadow.resource :as res]))

(defmacro default-parser
  "Based on instaparse's defparser"
  []
  (let [macro-time-parser (parser
                           (res/slurp-resource &env "grammar.ebnf")
                           :allow-namespaced-nts true
                           :auto-whitespace :standard)
        pre-processed-grammar (:grammar macro-time-parser)
        grammar-producing-code
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

             :else form))
         pre-processed-grammar)]
    `(parser ~grammar-producing-code
             :start :blazon
             :auto-whitespace :standard)))
