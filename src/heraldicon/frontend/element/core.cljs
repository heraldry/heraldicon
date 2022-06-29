(ns heraldicon.frontend.element.core
  (:require
   [heraldicon.context :as c]
   [heraldicon.interface :as interface]))

(defn- default-element [type]
  (case type
    :choice :select
    :boolean :checkbox
    :range :range
    :text :text-field
    nil))

(defmulti element (fn [context]
                    (let [options (interface/get-relevant-options context)]
                      (or
                       (-> options :ui :form-type)
                       (-> options :type default-element)))))

(defmethod element nil [context]
  (when-let [options (interface/get-relevant-options context)]
    [:div (str "not implemented: " context options)]))

(defn elements [context options]
  (into [:<>]
        (map (fn [option]
               ^{:key option} [element (c/++ context option)]))
        options))
