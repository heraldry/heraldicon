(ns heraldry.coat-of-arms.cottising
  (:require [heraldry.coat-of-arms.field.options :as field-options]
            [heraldry.coat-of-arms.line.core :as line]))

(def cottise-options
  {:enabled? {:type :boolean
              :default false}
   :line line/default-options
   :opposite-line (-> line/default-options
                      (assoc-in [:ui :label] "Opposite line"))
   :distance {:type :range
              :min -10
              :max 20
              :default 2
              :ui {:label "Distance"
                   :step 0.1}}
   :thickness {:type :range
               :min 0.1
               :max 20
               :default 2
               :ui {:label "Thickness"
                    :step 0.1}}
   :field field-options/default-options
   :ui {:form-type :cottising}})

(def options
  {:cottise-1 (-> cottise-options
                  (assoc-in [:ui :label] "Cottise 1"))
   :cottise-opposite-1 (-> cottise-options
                           (assoc-in [:ui :label] "Cottise opposite 1"))
   :cottise-2 (-> cottise-options
                  (assoc-in [:ui :label] "Cottise 2"))
   :cottise-opposite-2 (-> cottise-options
                           (assoc-in [:ui :label] "Cottise opposite 2"))})
