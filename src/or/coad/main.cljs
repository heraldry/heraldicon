(ns or.coad.main
  (:require ["js-base64" :as base64]
            [cljs-http.client :as http]
            [cljs.reader :as reader]
            [com.wsscode.common.async-cljs :refer [<? go-catch]]
            [goog.string.format]  ;; required for release build
            [or.coad.blazon :as blazon]
            [or.coad.division :as division]
            [or.coad.escutcheon :as escutcheon]
            [or.coad.field :as field]
            [or.coad.field-environment :as field-environment]
            [or.coad.filter :as filter]
            [or.coad.hatching :as hatching]
            [or.coad.line :as line]
            [or.coad.ordinary :as ordinary]
            [or.coad.tincture :as tincture]
            [re-frame.core :as rf]
            [reagent.dom :as r]))

;; subs


(rf/reg-sub
 :get
 (fn [db [_ & args]]
   (get-in db args)))

(rf/reg-sub
 :get-in
 (fn [db [_ path]]
   (get-in db path)))

(rf/reg-sub
 :get-division-type
 (fn [db [_ path]]
   (let [division (get-in db (concat path [:division :type]))]
     (or division :none))))

(rf/reg-sub
 :load-data
 (fn [db [_ name]]
   (let [data (get-in db [:loaded-data name])]
     (cond
       (= data :loading) nil
       data data
       :else (do
               (rf/dispatch-sync [:set :loaded-data name :loading])
               (println "fetching" name)
               (go-catch
                (->
                 (http/get name)
                 <?
                 :body
                 (as-> result
                       (rf/dispatch [:set :loaded-data name result]))))
               nil)))))

;; events


(def default-coat-of-arms
  {:escutcheon :heater
   :field {:content {:tincture :none}
           :charges [{:type :wolf
                      :attitude :rampant-regardant
                      :tincture {:primary :sable
                                 :armed :or
                                 :langued :gules
                                 :eyes-and-teeth :argent}
                      :variant :variant-wolf-rampant-regardant-1}]}})

(rf/reg-event-db
 :initialize-db
 (fn [db [_]]
   (merge {:rendering {:mode :colours
                       :outline :off}
           :coat-of-arms default-coat-of-arms} db)))

(rf/reg-event-db
 :set
 (fn [db [_ & args]]
   (assoc-in db (drop-last args) (last args))))

(rf/reg-event-db
 :set-in
 (fn [db [_ path value]]
   (assoc-in db path value)))

(rf/reg-event-db
 :set-division
 (fn [db [_ path new-type]]
   (if (= new-type :none)
     (-> db
         (update-in path dissoc :division)
         (update-in (conj path :content) #(or % {:tincture :none})))
     (-> db
         (assoc-in (concat path [:division :type]) new-type)
         (update-in (concat path [:division :line :style]) #(or % :straight))
         (update-in (concat path [:division :fields])
                    (fn [current-value]
                      (let [current (or current-value [])
                            current-type (get-in db (concat path [:division :type]))
                            current-mandatory-part-count (division/mandatory-part-count current-type)
                            new-mandatory-part-count (division/mandatory-part-count new-type)
                            min-mandatory-part-count (min current-mandatory-part-count
                                                          new-mandatory-part-count)
                            current (if (or (= current-mandatory-part-count
                                               new-mandatory-part-count)
                                            (<= (count current) min-mandatory-part-count))
                                      current
                                      (subvec current 0 min-mandatory-part-count))
                            default (into [{:content {:tincture :none}}
                                           {:content {:tincture :azure}}]
                                          (cond
                                            (#{:per-saltire :quarterly} new-type) [0 1]
                                            (= :gyronny new-type) [0 1 0 1 0 1]
                                            (#{:tierced-per-pale
                                               :tierced-per-fess
                                               :tierced-per-pairle
                                               :tierced-per-pairle-reversed} new-type) [{:content {:tincture :gules}}]))]
                        (cond
                          (< (count current) (count default)) (into current (subvec default (count current)))
                          (> (count current) (count default)) (subvec current 0 (count default))
                          :else current))))
         (update-in path dissoc :content)))))

(rf/reg-event-db
 :add-ordinary
 (fn [db [_ path value]]
   (update-in db (conj path :ordinaries) #(-> %
                                              (conj {:type value
                                                     :line {:style :straight}
                                                     :field {:content {:tincture :none}}})
                                              vec))))
(rf/reg-event-db
 :remove-ordinary
 (fn [db [_ path]]
   (let [ordinaries-path (drop-last path)
         index (last path)]
     (update-in db ordinaries-path (fn [ordinaries]
                                     (vec (concat (subvec ordinaries 0 index)
                                                  (subvec ordinaries (inc index)))))))))

;; views

(def defs
  (into
   [:defs
    filter/shadow
    filter/shiny
    [:pattern#void {:width 20
                    :height 20
                    :pattern-units "userSpaceOnUse"}
     [:rect {:x 0
             :y 0
             :width 20
             :height 20
             :fill "#fff"}]
     [:rect {:x 0
             :y 0
             :width 10
             :height 10
             :fill "#ddd"}]
     [:rect {:x 10
             :y 10
             :width 10
             :height 10
             :fill "#ddd"}]]]))

(defn render-coat-of-arms [data]
  (let [division (:division data)
        ordinaries (:ordinaries data)]
    [:<>
     [division/render division]
     (for [[idx ordinary] (map-indexed vector ordinaries)]
       ^{:key idx} [ordinary/render ordinary])]))

(defn render-shield [coat-of-arms options]
  (let [shield (escutcheon/field (:escutcheon coat-of-arms))
        environment (field-environment/transform-to-width shield 100)
        field (:field coat-of-arms)]
    [:g {:filter "url(#shadow)"}
     [:g {:transform "translate(10,10) scale(5,5)"}
      [:defs
       [:mask#mask-shield
        [:path {:d (:shape environment)
                :fill "#fff"
                :stroke "none"}]]]
      [:g {:mask "url(#mask-shield)"}
       [:path {:d (:shape environment)
               :fill "#f0f0f0"}]
       [field/render field environment options]]
      (when (:outline? options)
        [:path.outline {:d (:shape environment)}])]]))

(declare form-for-field)

(defn form-for-ordinary [path]
  [:<>
   [:a.remove {:on-click #(rf/dispatch [:remove-ordinary path])}
    "x"]
   [:div.ordinary
    [:div.setting
     [:label {:for "ordinary-type"} "Type"]
     [:select {:name "ordinary-type"
               :id "ordinary-type"
               :value (name @(rf/subscribe [:get-in (conj path :type)]))
               :on-change #(rf/dispatch [:set-in (conj path :type) (keyword (-> % .-target .-value))])}
      (for [[key display-name] ordinary/options]
        ^{:key key}
        [:option {:value (name key)} display-name])]]
    [:div.setting
     [:label {:for "line2"} "Line"]
     [:select {:name "line2"
               :id "line2"
               :value (name @(rf/subscribe [:get-in (concat path [:line :style])]))
               :on-change #(rf/dispatch [:set-in (concat path [:line :style]) (keyword (-> % .-target .-value))])}
      (for [[key display-name] line/options]
        ^{:key key}
        [:option {:value (name key)} display-name])]]
    [:div.parts
     [form-for-field (conj path :field)]]]])

(defn form-for-field [path]
  (let [division-type @(rf/subscribe [:get-division-type path])]
    [:div.field
     [:div.division
      [:div.setting
       [:label {:for "division-type"} "Division"]
       [:select {:name "division-type"
                 :id "division-type"
                 :value (name division-type)
                 :on-change #(rf/dispatch [:set-division path (keyword (-> % .-target .-value))])}
        (for [[key display-name] (into [[:none "None"]] division/options)]
          ^{:key key}
          [:option {:value (name key)} display-name])]]
      (when (not= division-type :none)
        [:<>
         [:div.line
          [:div.setting
           [:label {:for "line"} "Line"]
           [:select {:name "line"
                     :id "line"
                     :value (name @(rf/subscribe [:get-in (concat path [:division :line :style])]))
                     :on-change #(rf/dispatch [:set-in (concat path [:division :line :style]) (keyword (-> % .-target .-value))])}
            (for [[key display-name] line/options]
              ^{:key key}
              [:option {:value (name key)} display-name])]]]
         [:div.title "Parts"]
         [:div.parts
          (let [content @(rf/subscribe [:get-in (concat path [:division :fields])])
                mandatory-part-count (division/mandatory-part-count division-type)]
            (for [[idx part] (map-indexed vector content)]
              ^{:key idx}
              [:div.part
               (if (number? part)
                 [:<>
                  [:a.change {:on-click #(rf/dispatch [:set-in (concat path [:division :fields idx])
                                                       (get content part)])}
                   "o"]
                  [:span.same (str "Same as " (inc part))]]
                 [:<>
                  (when (>= idx mandatory-part-count)
                    [:a.remove {:on-click #(rf/dispatch [:set-in (concat path [:division :fields idx])
                                                         (mod idx mandatory-part-count)])}
                     "x"])
                  [form-for-field (vec (concat path [:division :fields idx]))]])]))]])]
     (when (= division-type :none)
       [:div.tincture
        [:div.setting
         [:label {:for "tincture"} "Tincture"]
         [:select {:name "tincture"
                   :id "tincture"
                   :value (name @(rf/subscribe [:get-in (concat path [:content :tincture])]))
                   :on-change #(rf/dispatch [:set-in (concat path [:content :tincture]) (keyword (-> % .-target .-value))])}
          [:option {:value "none"} "None"]
          (for [[group-name & options] tincture/options]
            ^{:key group-name}
            [:optgroup {:label group-name}
             (for [[display-name key] options]
               ^{:key key}
               [:option {:value (name key)} display-name])])]]])
     [:div.ordinaries-section
      [:div.title
       "Ordinaries"
       [:a.add {:on-click #(rf/dispatch [:add-ordinary path :pale])}
        "+"]]
      [:div.ordinaries
       (let [ordinaries @(rf/subscribe [:get-in (conj path :ordinaries)])]
         (for [[idx _] (map-indexed vector ordinaries)]
           ^{:key idx}
           [form-for-ordinary (vec (concat path [:ordinaries idx]))]))]]]))

(defn form-general []
  [:div.general {:style {:margin-bottom "1.5em"}}
   [:div.title "General"]
   [:div.setting
    [:label {:for "escutcheon"} "Escutcheon"]
    [:select {:name "escutcheon"
              :id "escutcheon"
              :value (name @(rf/subscribe [:get :coat-of-arms :escutcheon]))
              :on-change #(rf/dispatch [:set :coat-of-arms :escutcheon (keyword (-> % .-target .-value))])}
     (for [[key display-name] escutcheon/options]
       ^{:key key}
       [:option {:value (name key)} display-name])]]
   [:div.setting
    [:label {:for "mode"} "Mode"]
    [:select {:name "mode"
              :id "mode"
              :value (name @(rf/subscribe [:get :rendering :mode]))
              :on-change #(let [new-mode (keyword (-> % .-target .-value))]
                            (rf/dispatch [:set :rendering :mode new-mode])
                            (case new-mode
                              :hatching (rf/dispatch [:set :rendering :outline :on])
                              :colours (rf/dispatch [:set :rendering :outline :off])))}
     (for [[display-name key] [["Colours" :colours]
                               ["Hatching" :hatching]]]
       ^{:key key}
       [:option {:value (name key)} display-name])]]
   [:div.setting
    [:label {:for "outline"} "Outline"]
    [:select {:name "outline"
              :id "outline"
              :value (name @(rf/subscribe [:get :rendering :outline]))
              :on-change #(rf/dispatch [:set :rendering :outline (keyword (-> % .-target .-value))])}
     (for [[display-name key] [["On" :on]
                               ["Off" :off]]]
       ^{:key key}
       [:option {:value (name key)} display-name])]]])

(defn form []
  [:<>
   [form-general]
   [:div.title "Coat of Arms"]
   [form-for-field [:coat-of-arms :field]]])

(defn app []
  (fn []
    (let [coat-of-arms @(rf/subscribe [:get :coat-of-arms])
          mode @(rf/subscribe [:get :rendering :mode])
          options {:outline? (= @(rf/subscribe [:get :rendering :outline]) :on)
                   :mode mode}
          state-base64 (.encode base64 (prn-str coat-of-arms))]
      (when coat-of-arms
        (js/history.replaceState nil nil (str "#" state-base64)))
      [:<>
       [:div {:style {:width "100%"
                      :position "relative"}}
        [:svg {:id "svg"
               :style {:width "25em"
                       :height "30em"
                       :position "absolute"
                       :left 0
                       :top 0}
               :viewBox "0 0 520 1000"
               :preserveAspectRatio "xMidYMin slice"}
         defs
         (when (= mode :hatching)
           hatching/patterns)
         [render-shield coat-of-arms options]]
        [:div.blazonry {:style {:position "absolute"
                                :left 10
                                :top "31em"
                                :width "calc(25em - 20px)"
                                :height "5em"
                                :padding-left 10
                                :padding-right 10
                                :border "1px solid #ddd"}}
         [:span.disclaimer "Blazon (very rudimentary, very beta)"]
         [:div.blazon
          (blazon/encode-field (:field coat-of-arms) :root? true)]]
        [:div {:style {:position "absolute"
                       :left 10
                       :top "37em"
                       :width "25em"}}
         [:button {:on-click #(let [data (->>
                                          (js/document.getElementById "coa-data")
                                          .-value
                                          (.decode base64)
                                          reader/read-string)]
                                (rf/dispatch [:set :coat-of-arms data]))}
          "Load"]
         [:button {:on-click #(do
                                (->>
                                 (js/document.getElementById "coa-data")
                                 .select)
                                (js/document.execCommand "copy"))}
          "Copy"]
         [:button {:on-click #(->
                               (js/navigator.clipboard.readText)
                               (.then (fn [text]
                                        (-> (js/document.getElementById "coa-data")
                                            .-value
                                            (set! text)))))}
          "Paste"]
         [:button {:on-click #(rf/dispatch-sync [:set :coat-of-arms default-coat-of-arms])}
          "Reset"]
         [:textarea {:id "coa-data"
                     :cols 100
                     :rows 10
                     :style {:width "100%"}
                     :value state-base64
                     ;; TODO: this doesn't seem to work right now
                     :on-change (fn [event]
                                  (-> (js/document.getElementById "coa-data")
                                      .-value
                                      (set! (-> event .-target .-value))))}]]
        [:div {:style {:position "absolute"
                       :left "27em"
                       :top 0
                       :width "calc(100vw - 27em)"
                       :height "100vh"
                       :overflow "auto"}}
         [form]]]])))

(defn stop []
  (println "Stopping..."))

(defn start []
  (rf/dispatch-sync [:initialize-db])
  (let [hash (subs js/location.hash 1)]
    (when (> (count hash) 0)
      (let [data (->>
                  hash
                  (.decode base64)
                  reader/read-string)]
        (rf/dispatch-sync [:set :coat-of-arms data]))))
  (r/render [app]
            (.getElementById js/document "app")))

(defn ^:export init []
  (start))
