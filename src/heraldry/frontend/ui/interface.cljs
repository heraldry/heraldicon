(ns heraldry.frontend.ui.interface
  (:require
   [heraldry.coat-of-arms.counterchange :as counterchange]
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.options :as options]
   [re-frame.core :as rf]
   [taoensso.timbre :as log]))

;; component-node-data

(defmulti component-node-data interface/effective-component-type)

(defmethod component-node-data nil [context]
  (log/warn :not-implemented "component-node-data" context)
  {:title (str "unknown")})

;; component-form-data

(defmulti component-form-data interface/effective-component-type)

(defmethod component-form-data nil [_context]
  {:form (fn [_context]
           [:<>])})

;; form-element

(defn default-element [type]
  (case type
    :choice :select
    :boolean :checkbox
    :range :range
    :text :text-field
    nil))

(rf/reg-sub :get-sanitized-data
  (fn [[_ path] _]
    [(rf/subscribe [:get path])
     (rf/subscribe [:get-relevant-options path])])

  (fn [[data options] [_ _path]]
    (options/sanitize-value-or-data data options)))

(rf/reg-sub :get-counterchange-tinctures
  (fn [[_ path _context] _]
    (rf/subscribe [:get path]))

  (fn [data [_ _path context]]
    (counterchange/get-counterchange-tinctures data context)))

(defmulti form-element (fn [context]
                         (let [options (interface/get-relevant-options context)]
                           (or
                            (-> options :ui :form-type)
                            (-> options :type default-element)))))

(defmethod form-element nil [context]
  (when-let [options (interface/get-relevant-options context)]
    [:div (str "not implemented: " context options)]))

(defn form-elements [context options]
  [:<>
   (doall
    (for [option options]
      ^{:key option} [form-element (c/++ context option)]))])
