(ns heraldry.frontend.ui.interface
  (:require [heraldry.coat-of-arms.counterchange :as counterchange]
            [heraldry.interface :as interface]
            [heraldry.options :as options]
            [re-frame.core :as rf]))

;; component-node-data

(defmulti component-node-data (fn [path]
                                (interface/effective-component-type
                                 path
                                 @(rf/subscribe [:get-value (conj path :type)]))))

(defmethod component-node-data nil [_path]
  {:title "unknown"})

;; component-form-data

(defmulti component-form-data (fn [path]
                                (interface/effective-component-type
                                 path
                                 @(rf/subscribe [:get-value (conj path :type)]))))

(defmethod component-form-data nil [_path]
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

(rf/reg-sub :get-options
  (fn [[_ path] _]
    (rf/subscribe [:get-value path]))

  (fn [data [_ path]]
    (interface/component-options path data)))

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

(rf/reg-sub :get-sanitized-data
  (fn [[_ path] _]
    [(rf/subscribe [:get-value path])
     (rf/subscribe [:get-relevant-options path])])

  (fn [[data options] [_ _path]]
    (options/sanitize-value-or-data data options)))

(rf/reg-sub :get-counterchange-tinctures
  (fn [[_ path _context] _]
    (rf/subscribe [:get-value path]))

  (fn [data [_ _path context]]
    (counterchange/get-counterchange-tinctures data context)))

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
