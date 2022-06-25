(ns heraldicon.frontend.ui.blazonry-editor.core
  (:require
   [clojure.walk :as walk]
   [heraldicon.context :as c]
   [heraldicon.frontend.context :as context]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.modal :as modal]
   [heraldicon.frontend.repository.entity-list :as entity-list]
   [heraldicon.frontend.ui.auto-complete :as auto-complete]
   [heraldicon.frontend.ui.blazonry-editor.editor :as editor]
   [heraldicon.frontend.ui.blazonry-editor.help :as help]
   [heraldicon.frontend.ui.blazonry-editor.parser :as parser]
   [heraldicon.frontend.ui.blazonry-editor.shared :as shared]
   [heraldicon.heraldry.default :as default]
   [heraldicon.render.core :as render]
   [re-frame.core :as rf]))

(def ^:private hdn-path
  (conj shared/blazonry-editor-path :hdn))

(defn- clean-field-data [data]
  (walk/postwalk
   (fn [data]
     (if (map? data)
       (dissoc data
               :heraldicon.reader.blazonry.transform/warnings
               :heraldicon.reader.blazonry.process/warnings)
       data))
   data))

(rf/reg-event-fx ::apply-blazon-result
  (fn [{:keys [db]} [_ {:keys [path]}]]
    (let [field-data (clean-field-data (get-in db (conj hdn-path :coat-of-arms :field)))]
      {:db (assoc-in db path field-data)
       :dispatch [::modal/clear]})))

(rf/reg-event-db ::set-hdn-escutcheon
  (fn [db [_ context]]
    (let [escutcheon (if (->> context
                              :path
                              (take-last 2)
                              (= [:coat-of-arms :field]))
                       ;; TODO: rather complex and convoluted
                       (let [escutcheon-db-path (-> context
                                                    (c/-- 2)
                                                    (c/++ :render-options :escutcheon)
                                                    :path)]
                         (get-in db escutcheon-db-path :heater))
                       :rectangle)]
      (assoc-in db hdn-path (update default/achievement :render-options
                                    merge {:escutcheon escutcheon
                                           :outline? true})))))

(defn open [context]
  (rf/dispatch-sync [::editor/clear])
  (rf/dispatch [::entity-list/load-if-absent :heraldicon.entity.type/charge #(rf/dispatch [::parser/update %])])
  (rf/dispatch-sync [::set-hdn-escutcheon context])
  (modal/create
   [:div
    [tr :string.button/from-blazon]
    [:div.tooltip.info {:style {:display "inline-block"
                                :margin-left "0.2em"}}
     [:sup {:style {:color "#d40"}}
      "alpha"]
     [:div.bottom
      [:p [tr :string.tooltip/alpha-feature-warning]]]]]
   [(fn []
      [:div
       [:div {:style {:display "flex"
                      :flex-flow "row"
                      :height "30em"}}
        [help/help]
        [:div {:style {:display "flex"
                       :flex-flow "column"
                       :flex "auto"
                       :height "100%"
                       :margin-left "10px"}}
         [:div {:style {:width "35em"
                        :display "flex"
                        :flex-flow "row"}}
          [:div {:style {:width "20em"
                         :height "100%"
                         :margin-right "10px"}}
           [editor/editor]]
          [:div {:style {:width "15em"
                         :height "100%"}}
           [render/achievement
            (assoc context/default
                   :path hdn-path
                   :render-options-path (conj hdn-path :render-options))]]]
         [:div {:style {:height "100%"
                        :margin-top "10px"
                        :overflow-y "scroll"}}
          [parser/status]]]]

       [:div.buttons {:style {:display "flex"}}
        [:div {:style {:flex "auto"}}]
        [:button.button
         {:type "button"
          :style {:flex "initial"
                  :margin-left "10px"}
          :on-click (fn [_]
                      (rf/dispatch [::auto-complete/clear])
                      (modal/clear))}
         [tr :string.button/cancel]]

        [:button.button.primary {:type "submit"
                                 :on-click #(rf/dispatch [::apply-blazon-result context])
                                 :style {:flex "initial"
                                         :margin-left "10px"}}
         [tr :string.button/apply]]]])]
   :on-cancel #(rf/dispatch [::auto-complete/clear])))
