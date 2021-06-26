(ns heraldry.frontend.ui.interface
  (:require [clojure.string :as s]))

(defn type->component-type [t]
  (let [ts (str t)]
    (cond
      (s/starts-with? ts ":heraldry.component") t
      (s/starts-with? ts ":heraldry.field") :heraldry.component/field
      (s/starts-with? ts ":heraldry.ordinary") :heraldry.component/ordinary
      (s/starts-with? ts ":heraldry.charge") :heraldry.component/charge
      :else :heraldry.component/unknown)))

(defn effective-component-type [data]
  (cond
    (map? data) (-> data :type type->component-type)
    (vector? data) :heraldry.component/items
    :else :heraldry.component/unknown))

;; component-node-data

(defmulti component-node-data (fn [_path component-data]
                                (effective-component-type component-data)))

(defmethod component-node-data :heraldry.component/items [path component-data]
  {:nodes (->> component-data
               count
               range
               (map (fn [idx]
                      {:path (conj path idx)}))
               vec)
   :selectable? false})

(defmethod component-node-data :heraldry.component/unknown [_path _component-data]
  {:title "unknown"})

;; component-form-data

(defmulti component-form-data (fn [component-data]
                                (effective-component-type component-data)))

(defmethod component-form-data :heraldry.component/unknown [_path _component-data]
  {:form (fn [_path _form-data]
           [:div])
   :form-args {}})

;; form-element

(defn default-element [type]
  (case type
    :choice :select
    :boolean :checkbox
    :range :range
    nil))

(defmulti form-element (fn [_path {:keys [type ui]}]
                         (or (:form-type ui)
                             (default-element type))))

(defmethod form-element nil [_path option]
  (when option
    [:div (str "not implemented: " (:type option) (-> option :ui :form-type))]))
