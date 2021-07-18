(ns heraldry.frontend.ui.interface
  (:require [clojure.string :as s]
            [heraldry.coat-of-arms.options :as options]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(defn type->component-type [t]
  (let [ts (str t)]
    (cond
      (s/starts-with? ts ":heraldry.component/cottise") :heraldry.component/cottise
      (s/starts-with? ts ":heraldry.component") t
      (s/starts-with? ts ":heraldry.field") :heraldry.component/field
      (s/starts-with? ts ":heraldry.ordinary") :heraldry.component/ordinary
      (s/starts-with? ts ":heraldry.charge-group") :heraldry.component/charge-group
      (s/starts-with? ts ":heraldry.charge") :heraldry.component/charge
      :else :heraldry.component/unknown)))

(defn effective-component-type [path data]
  (cond
    (-> path last (= :arms-form)) :heraldry.component/arms-general
    (-> path last (= :charge-form)) :heraldry.component/charge-general
    (-> path last (= :collection-form)) :heraldry.component/collection-general
    (-> path last (= :collection)) :heraldry.component/collection
    (-> path drop-last (->> (take-last 2)) (= [:collection :elements])) :heraldry.component/collection-element
    (map? data) (-> data :type type->component-type)
    :else :heraldry.component/unknown))

;; component-node-data

(defmulti component-node-data (fn [path component-data _component-options]
                                (effective-component-type path component-data)))

(defmethod component-node-data :heraldry.component/unknown [_path _component-data _component-options]
  {:title "unknown"})

;; component-form-data

(defmulti component-form-data (fn [path component-data _component-options]
                                (effective-component-type path component-data)))

(defmethod component-form-data :heraldry.component/unknown [_path _component-data _component-options]
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
          (= :arms-form)) :arms-general
      (-> path last
          (= :charge-form)) :charge-general
      (-> path last
          (= :collection-form)) :collection-general
      (-> path last
          (= :collection)) :collection
      (-> path drop-last
          (->> (take-last 2))
          (= [:collection :elements])) :collection-element
      (-> path last
          (= :render-options)) :render-options
      (-> path last
          (= :coat-of-arms)) :coat-of-arms
      :else (let [ts (-> data :type str)]
              (cond
                (s/starts-with? ts ":heraldry.field") :field
                (s/starts-with? ts ":heraldry.ordinary") :ordinary
                (s/starts-with? ts ":heraldry.component/charge-group-strip") :charge-group-strip
                (s/starts-with? ts ":heraldry.charge-group") :charge-group
                (s/starts-with? ts ":heraldry.charge") :charge
                (s/starts-with? ts ":heraldry.component/semy") :semy
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
    (or (->> (range (count path) 0 -1)
             (keep (fn [idx]
                     (let [option-path (subvec path 0 idx)
                           relative-path (subvec path idx)
                           sub (rf/subscribe [:get-options option-path])]
                       (when @sub
                         [sub (r/atom relative-path)]))))
             first)
        [(r/atom nil) (r/atom nil)]))

  (fn [[relevant-options relative-path] [_ _path]]
    (get-in relevant-options relative-path)))

(rf/reg-sub :get-sanitized-value
  (fn [[_ path] _]
    [(rf/subscribe [:get-value path])
     (rf/subscribe [:get-relevant-options path])])

  (fn [[value options] [_ _path]]
    (if (map? value)
      (options/sanitize value options)
      (options/get-value value options))))

(rf/reg-sub :get-form-element-type
  (fn [[_ path] _]
    (rf/subscribe [:get-relevant-options path]))

  (fn [options [_ _path]]
    (or
     (-> options :ui :form-type)
     (-> options :type default-element))))

(defmulti form-element (fn [path]
                         @(rf/subscribe [:get-form-element-type path])))

(defmethod form-element nil [path]
  (when-let [options @(rf/subscribe [:get-relevant-options path])]
    [:div (str "not implemented: " path options)]))
