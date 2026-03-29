(ns heraldicon.heraldry.field.interface
  (:require
   [heraldicon.context :as c]
   [heraldicon.interface :as interface]
   [taoensso.timbre :as log]))

(def ^:private tinctures-only-field-types
  #{:chequy
    :endente
    :fretty
    :lozengy
    :masony
    :papellony
    :plumetty
    :potenty
    :scaly
    :vairy})

(defn tinctures-only? [field-type]
  (-> field-type name keyword tinctures-only-field-types))

(defmulti display-name identity)

(defmulti part-names identity)

(defmulti render-field (fn [context]
                         (interface/get-sanitized-data (c/++ context :type))))

(defmethod render-field nil [context]
  (log/warn :not-implemented "render-field" context)
  [:<>])

(defmulti options interface/effective-component-type)

(defmethod options nil [context]
  (log/warn :not-implemented "field.options" context)
  [:<>])
