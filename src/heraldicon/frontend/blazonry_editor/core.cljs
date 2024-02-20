(ns heraldicon.frontend.blazonry-editor.core
  (:require
   [clojure.walk :as walk]
   [heraldicon.context :as c]
   [heraldicon.frontend.auto-complete :as auto-complete]
   [heraldicon.frontend.blazonry-editor.editor :as editor]
   [heraldicon.frontend.blazonry-editor.help :as help]
   [heraldicon.frontend.blazonry-editor.parser :as parser]
   [heraldicon.frontend.blazonry-editor.shared :as shared]
   [heraldicon.frontend.context :as context]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.modal :as modal]
   [heraldicon.frontend.repository.blazonry-parser-data :as blazonry-parser-data]
   [heraldicon.frontend.tooltip :as tooltip]
   [heraldicon.heraldry.default :as default]
   [heraldicon.interface :as interface]
   [heraldicon.reader.blazonry.result :as result]
   [re-frame.core :as rf]))

(def ^:private hdn-path
  (conj shared/blazonry-editor-path :hdn))

(defn- clean-field-data [data]
  (walk/postwalk
   (fn [data]
     (if (map? data)
       (dissoc data ::result/warnings)
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
  @(rf/subscribe [::blazonry-parser-data/data #(rf/dispatch [::parser/update %])])
  (rf/dispatch-sync [::set-hdn-escutcheon context])
  (modal/create
   [:div
    [tr :string.button/from-blazon]
    [tooltip/info :string.tooltip/alpha-feature-warning
     :element [:sup {:style {:color "#d40"}}
               "alpha"]]]
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
           [interface/render-component (assoc context/default
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
