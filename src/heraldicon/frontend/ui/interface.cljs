(ns heraldicon.frontend.ui.interface
  (:require
   [heraldicon.coat-of-arms.counterchange :as counterchange]
   [heraldicon.context :as c]
   [heraldicon.interface :as interface]
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
