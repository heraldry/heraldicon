(ns heraldicon.frontend.ui.element.select
  (:require
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.ui.element.value-mode-select :as value-mode-select]
   [heraldicon.frontend.ui.interface :as ui.interface]
   [heraldicon.interface :as interface]
   [heraldicon.util :as util]
   [heraldicon.util.uid :as uid]
   [re-frame.core :as rf]))

(defn raw-select-inline [context value choices & {:keys [on-change component-id keywordize?]
                                                  :or {keywordize? true}}]
  [:select {:id component-id
            :value (if keywordize?
                     (util/keyword->str value)
                     value)
            :on-change #(let [selected (cond-> (-> % .-target .-value)
                                         keywordize? keyword)]
                          (if on-change
                            (on-change selected)
                            (rf/dispatch [:set context selected])))}
   (doall
    (for [[group-name & group-choices] choices]
      (if (and (-> group-choices count (= 1))
               (-> group-choices first vector? not))
        (let [key (-> group-choices first)]
          ^{:key key}
          [:option {:value (if keywordize?
                             (util/keyword->str key)
                             key)}
           (tr group-name)])
        ^{:key group-name}
        [:optgroup {:label (tr group-name)}
         (doall
          (for [[display-name key] group-choices]
            ^{:key key}
            [:option {:value (if keywordize?
                               (util/keyword->str key)
                               key)}
             (tr display-name)]))])))])

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

(defn select [context & {:keys [on-change]}]
  (when-let [option (interface/get-relevant-options context)]
    (let [current-value (interface/get-raw-data context)
          {:keys [ui default inherited choices]} option
          label (:label ui)
          value (or current-value
                    inherited
                    default
                    :none)]
      [raw-select context value label choices :on-change on-change])))

(defmethod ui.interface/form-element :select [context]
  [select context])
