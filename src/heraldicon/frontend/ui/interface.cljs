(ns heraldicon.frontend.ui.interface
  (:require
   [heraldicon.context :as c]
   [heraldicon.interface :as interface]
   [taoensso.timbre :as log]))

(defmulti component-node-data interface/effective-component-type)

(defmethod component-node-data nil [context]
  (log/warn :not-implemented "component-node-data" context)
  {:title (str "unknown")})

(defmulti component-form-data interface/effective-component-type)

(defmethod component-form-data nil [_context]
  {:form (constantly [:<>])})

(defn- default-element [type]
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
  (into [:<>]
        (map (fn [option]
               ^{:key option} [form-element (c/++ context option)]))
        options))
