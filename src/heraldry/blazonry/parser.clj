(ns heraldry.blazonry.parser
  (:require
   [shadow.resource :as res]))

(defmacro load-grammar []
  (res/slurp-resource &env "grammar.ebnf"))
