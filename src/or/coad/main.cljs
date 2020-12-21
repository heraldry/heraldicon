(ns or.coad.main
  (:require [goog.string.format]  ;; required for release build
            [or.coad.division :as division]
            [or.coad.escutcheon :as escutcheon]
            [or.coad.field :as field]
            [or.coad.field-content :as field-content]
            [or.coad.filter :as filter]
            [or.coad.line :as line]
            [or.coad.ordinary :as ordinary]
            [re-frame.core :as rf]
            [reagent.dom :as r]))

                                        ; subs


(rf/reg-sub
 :get
 (fn [db [_ & args]]
   (get-in db args)))

(rf/reg-sub
 :get-in
 (fn [db [_ path]]
   (get-in db path)))

                                        ; events

(rf/reg-event-db
 :initialize-db
 (fn [db [_]]
   (merge {:coat-of-arms {:escutcheon :heater
                          :content {:division {:type :per-pale
                                               :line {:style :straight}
                                               :content [{:content {:tincture :azure}}
                                                         {:content {:tincture :sable}}
                                                         {:content {:tincture :gules}}]}
                                    :ordinaries [{:type :pale
                                                  :line {:style :straight}
                                                  :content {:content {:tincture :or}}}]}}} db)))

(rf/reg-event-db
 :set
 (fn [db [_ & args]]
   (assoc-in db (drop-last args) (last args))))

(rf/reg-event-db
 :set-in
 (fn [db [_ path value]]
   (assoc-in db path value)))
                                        ; views

(def defs
  (into
   [:defs
    filter/shadow
    filter/shiny]))

(defn render-coat-of-arms [data]
  (let [division (:division data)
        ordinaries (:ordinaries data)]
    [:<>
     [division/render division]
     (for [[idx ordinary] (map-indexed vector ordinaries)]
       ^{:key idx} [ordinary/render ordinary])]))

(defn render-shield [coat-of-arms]
  (let [field (escutcheon/field (:escutcheon coat-of-arms))
        transformed-field (field/transform-to-width field 100)
        content (:content coat-of-arms)]
    [:g {:filter "url(#shadow)"}
     [:g {:transform "translate(10,10) scale(5,5)"}
      [:defs
       [:mask#mask-shield
        [:path {:d (:shape transformed-field)
                :fill "#fff"
                :stroke "none"}]]]
      [:g {:mask "url(#mask-shield)"}
       [:path {:d (:shape transformed-field)
               :fill "#f0f0f0"}]
       [field-content/render content transformed-field]]]]))

(defn form-for-field [path]
  [:div
   [:fieldset
    [:label {:for "division"} "Division"]
    [:select {:name "division"
              :id "division"
              :value (name @(rf/subscribe [:get-in (concat path [:division :type])]))
              :on-change #(rf/dispatch [:set-in (concat path [:division :type]) (keyword (-> % .-target .-value))])}
     (for [[key display-name] division/options]
       ^{:key key}
       [:option {:value (name key)} display-name])]]
   [:fieldset
    [:label {:for "line"} "Line"]
    [:select {:name "line"
              :id "line"
              :value (name @(rf/subscribe [:get-in (concat path [:division :line :style])]))
              :on-change #(rf/dispatch [:set-in (concat path [:division :line :style]) (keyword (-> % .-target .-value))])}
     (for [[key display-name] line/options]
       ^{:key key}
       [:option {:value (name key)} display-name])]]
   [:fieldset
    [:label {:for "ordinary"} "Ordinary"]
    [:select {:name "ordinary"
              :id "ordinary"
              :value (name @(rf/subscribe [:get-in (concat path [:ordinaries 0 :type])]))
              :on-change #(rf/dispatch [:set-in (concat path [:ordinaries 0 :type]) (keyword (-> % .-target .-value))])}
     (for [[key display-name] ordinary/options]
       ^{:key key}
       [:option {:value (name key)} display-name])]]
   [:fieldset
    [:label {:for "line2"} "Line"]
    [:select {:name "line2"
              :id "line2"
              :value (name @(rf/subscribe [:get-in (concat path [:ordinaries 0 :line :style])]))
              :on-change #(rf/dispatch [:set-in (concat path [:ordinaries 0 :line :style]) (keyword (-> % .-target .-value))])}
     (for [[key display-name] line/options]
       ^{:key key}
       [:option {:value (name key)} display-name])]]])

(defn form []
  [:div.form
   [:fieldset
    [:label {:for "escutcheon"} "Escutcheon"]
    [:select {:name "escutcheon"
              :id "escutcheon"
              :value (name @(rf/subscribe [:get :coat-of-arms :escutcheon]))
              :on-change #(rf/dispatch [:set :coat-of-arms :escutcheon (keyword (-> % .-target .-value))])}
     (for [[key display-name] escutcheon/options]
       ^{:key key}
       [:option {:value (name key)} display-name])]]
   [form-for-field [:coat-of-arms :content]]])

(defn app []
  (fn []
    (let [coat-of-arms @(rf/subscribe [:get :coat-of-arms])]
      [:<>
       [:div {:style {:width "100%"
                      :position "relative"}}
        [:svg {:id "svg"
               :style {:width "30em"
                       :position "absolute"
                       :left 0
                       :top 0}
               :viewBox "0 0 600 1000"
               :preserveAspectRatio "xMidYMin slice"}
         defs
         [render-shield coat-of-arms]]
        [:div {:style {:position "absolute"
                       :left "30em"
                       :top 0}}
         [form]]]])))

(defn stop []
  (println "Stopping..."))

(defn start []
  (rf/dispatch-sync [:initialize-db])
  (r/render [app]
            (.getElementById js/document "app")))

(defn ^:export init []
  (start))
