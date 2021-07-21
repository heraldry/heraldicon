(ns heraldry.coat-of-arms.cottising
  (:require [heraldry.coat-of-arms.default :as default]
            [heraldry.coat-of-arms.field.options :as field-options]
            [heraldry.coat-of-arms.line.core :as line]))

(def cottise-default-options
  {:line (-> line/default-options
             (assoc-in [:ui :label] "Line"))
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
   :field (field-options/options default/field)
   :ui {:form-type :cottising}})

(def default-options
  {:cottise-1 (-> cottise-default-options
                  (assoc-in [:ui :label] "Cottise 1"))
   :cottise-opposite-1 (-> cottise-default-options
                           (assoc-in [:ui :label] "Cottise opposite 1"))
   :cottise-2 (-> cottise-default-options
                  (assoc-in [:ui :label] "Cottise 2"))
   :cottise-opposite-2 (-> cottise-default-options
                           (assoc-in [:ui :label] "Cottise opposite 2"))})

(defn cottise-options [options {:keys [line opposite-line field]}]
  (cond-> options
    (and (:line options)
         line)
    (assoc :line (-> (line/options line)
                     (assoc :ui (-> cottise-default-options :line :ui))))

    (and (:opposite-line options)
         opposite-line)
    (assoc :opposite-line (-> (line/options opposite-line)
                              (assoc :ui (-> cottise-default-options :opposite-line :ui))))

    (and (:field options)
         field)
    (assoc :field (field-options/options field))))

(defn options [options {:keys [cottise-1 cottise-2
                               cottise-opposite-1 cottise-opposite-2]}]
  (cond-> options
    (and (:cottise-1 options)
         cottise-1)
    (update :cottise-1 cottise-options cottise-1)

    (and (:cottise-2 options)
         cottise-2)
    (update :cottise-2 cottise-options cottise-2)

    (and (:cottise-opposite-1 options)
         cottise-opposite-1)
    (update :cottise-opposite-1 cottise-options cottise-opposite-1)

    (and (:cottise-opposite-2 options)
         cottise-opposite-2)
    (update :cottise-opposite-2 cottise-options cottise-opposite-2)))
