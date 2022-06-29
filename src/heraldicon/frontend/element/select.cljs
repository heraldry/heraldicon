(ns heraldicon.frontend.element.select
  (:require
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.element.value-mode-select :as value-mode-select]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.interface :as interface]
   [heraldicon.util.core :as util]
   [heraldicon.util.uid :as uid]
   [re-frame.core :as rf]))

(defn raw-select-inline [context value choices & {:keys [on-change component-id keywordize?]
                                                  :or {keywordize? true}}]
  (into [:select {:id component-id
                  :value (if keywordize?
                           (util/keyword->str value)
                           value)
                  :on-change #(let [selected (cond-> (-> % .-target .-value)
                                               keywordize? keyword)]
                                (if on-change
                                  (on-change selected)
                                  (rf/dispatch [:set context selected])))}]
        (map (fn [[group-name & group-choices]]
               (if (and (-> group-choices count (= 1))
                        (-> group-choices first vector? not))
                 (let [key (first group-choices)]
                   ^{:key key}
                   [:option {:value (if keywordize?
                                      (util/keyword->str key)
                                      key)}
                    (tr group-name)])
                 (into
                  ^{:key group-name}
                  [:optgroup {:label (tr group-name)}]
                  (map (fn [[display-name key]]
                         ^{:key key}
                         [:option {:value (if keywordize?
                                            (util/keyword->str key)
                                            key)}
                          (tr display-name)]))
                  group-choices))))
        choices))

(defn raw-select [context value label choices & {:keys [on-change]}]
  (let [component-id (uid/generate "select")]
    [:div.ui-setting
     (when label
       [:label {:for component-id} [tr label]])
     [:div.option
      [raw-select-inline context value choices
       :on-change on-change
       :component-id component-id]
      [value-mode-select/value-mode-select context]]]))

(defn- select [context & {:keys [on-change]}]
  (when-let [option (interface/get-relevant-options context)]
    (let [current-value (interface/get-raw-data context)
          {:keys [ui default inherited choices]} option
          label (:label ui)
          value (or current-value
                    inherited
                    default
                    :none)]
      [raw-select context value label choices :on-change on-change])))

(defmethod element/element :select [context]
  [select context])
