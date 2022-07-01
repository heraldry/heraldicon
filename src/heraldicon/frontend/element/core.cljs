(ns heraldicon.frontend.element.core
  (:require
   [heraldicon.context :as c]
   [heraldicon.interface :as interface]))

(def ^:private default-element
  {:option.type/choice :ui.element/select
   :option.type/boolean :ui.element/checkbox
   :option.type/range :ui.element/range
   :option.type/text :ui.element/text-field})

(defmulti element (fn [context]
                    (let [{:keys [type]
                           :ui/keys [element]} (interface/get-relevant-options context)]
                      (or element (get default-element type)))))

(defmethod element nil [context]
  (when-let [options (interface/get-relevant-options context)]
    [:div (str "not implemented: " context options)]))

(defn elements [context options]
  (into [:<>]
        (map (fn [option]
               ^{:key option} [element (c/++ context option)]))
        options))
