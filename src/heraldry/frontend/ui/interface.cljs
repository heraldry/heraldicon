(ns heraldry.frontend.ui.interface
  (:require [clojure.string :as s]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(defn type->component-type [t]
  (let [ts (str t)]
    (cond
      (s/starts-with? ts ":heraldry.component") t
      (s/starts-with? ts ":heraldry.field") :heraldry.component/field
      (s/starts-with? ts ":heraldry.ordinary") :heraldry.component/ordinary
      (s/starts-with? ts ":heraldry.charge-group") :heraldry.component/charge-group
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
    :text :text-field
    nil))

(defmulti component-options
  (fn [data path]
    (cond
      (-> path last
          (= :render-options)) :render-options
      (-> path last
          (= :coat-of-arms)) :coat-of-arms
      (-> path last
          (= :attribution)) :attribution
      :else (let [ts (-> data :type str)]
              (cond
                (s/starts-with? ts ":heraldry.field") :field
                (s/starts-with? ts ":heraldry.component/charge-group-strip") :charge-group-strip
                (s/starts-with? ts ":heraldry.charge-group") :charge-group
                (s/starts-with? ts ":heraldry.charge") :charge
                :else nil)))))

(defmethod component-options nil [_data _path]
  nil)

(rf/reg-sub :get-options
  (fn [[_ path] _]
    (rf/subscribe [:get-value path]))

  (fn [data [_ path]]
    (component-options data path)))

(rf/reg-sub :get-relevant-options
  (fn [[_ path] _]
    (or (->> (range 1 (count path))
             (keep (fn [idx]
                     (let [option-path (subvec path 0 idx)
                           relative-path (subvec path idx)
                           subscription (rf/subscribe [:get-options option-path])]
                       (when (get-in @subscription relative-path)
                         [subscription (r/atom relative-path)]))))
             first)
        [(r/atom nil) (r/atom nil)]))

  (fn [[options relative-path] [_ _path]]
    (get-in options relative-path)))

(rf/reg-sub :get-form-element-type
  (fn [[_ path] _]
    (rf/subscribe [:get-relevant-options path]))

  (fn [options [_ _path]]
    (or
     (-> options :ui :form-type)
     (-> options :type default-element))))

(defmulti form-element (fn [path {:keys [type ui]}]
                         (or @(rf/subscribe [:get-form-element-type path])
                             (:form-type ui)
                             (default-element type))))

(defmethod form-element nil [_path option]
  (when option
    [:div (str "not implemented: " (:type option) (-> option :ui :form-type))]))
