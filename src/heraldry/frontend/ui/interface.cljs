(ns heraldry.frontend.ui.interface
  (:require [clojure.string :as s]
            [re-frame.core :as rf]))

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
          (= :render-options)) :render-options
      (-> path last
          (= :coat-of-arms)) :coat-of-arms
      (-> path last
          (= :attribution)) :attribution
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
  (fn [_ [_ path]]
    ;; TODO: can this be done by feeding the subscriptions in again?
    ;; probably is more efficient, but the previous attempt didn't refresh the
    ;; subscription properly when the options changed (e.g. switching to "arc" in a charge-group)
    (->> (range (count path) 0 -1)
         (keep (fn [idx]
                 (let [option-path (subvec path 0 idx)
                       relative-path (subvec path idx)
                       options @(rf/subscribe [:get-options option-path])]
                   (when-let [relevant-options (get-in options relative-path)]
                     relevant-options))))
         first)))

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
    [:div (str "not implemented: " (:type options) (-> options :ui :form-type))]))
