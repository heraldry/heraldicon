(ns or.coad.main
  (:require [goog.string.format]  ;; required for release build
            [or.coad.division :as division]
            [or.coad.escutcheon :as escutcheon]
            [or.coad.field :as field]
            [or.coad.field-content :as field-content]
            [or.coad.filter :as filter]
            [or.coad.line :as line]
            [or.coad.ordinary :as ordinary]
            [or.coad.tincture :as tincture]
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

(rf/reg-sub
 :get-division
 (fn [db [_ path]]
   (let [division (get-in db (concat path [:division :type]))]
     (or division :none))))

                                        ; events

(rf/reg-event-db
 :initialize-db
 (fn [db [_]]
   (merge {:coat-of-arms {:escutcheon :heater
                          :content {:content {:tincture :argent}}}} db)))

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
 (fn [db [_ path value]]
   (if (= value :none)
     (-> db
         (update-in path dissoc :division)
         (assoc-in (conj path :content) {:tincture :argent}))
                                        ; TODO:
                                        ; - set :content if this is being created
                                        ; - also add/remove things from :content as necessary
     (-> db
         (assoc-in (concat path [:division :type]) value)
         (assoc-in (concat path [:division :line :style]) :straight)
         (assoc-in (concat path [:division :content]) [{:content {:tincture :argent}}
                                                       {:content {:tincture :azure}}
                                                       {:content {:tincture :or}}])
         (update-in path dissoc :content)))))

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
  (let [division @(rf/subscribe [:get-division path])]
    [:div.field
     [:div.division
      [:label {:for "division"} "Division"]
      [:select {:name "division"
                :id "division"
                :value (name division)
                :on-change #(rf/dispatch [:set-division path (keyword (-> % .-target .-value))])}
       (for [[key display-name] (into [[:none "None"]] division/options)]
         ^{:key key}
         [:option {:value (name key)} display-name])]

      (if (not= division :none)
        [:div
         [:label {:for "line"} "Line"]
         [:select {:name "line"
                   :id "line"
                   :value (name @(rf/subscribe [:get-in (concat path [:division :line :style])]))
                   :on-change #(rf/dispatch [:set-in (concat path [:division :line :style]) (keyword (-> % .-target .-value))])}
          (for [[key display-name] line/options]
            ^{:key key}
            [:option {:value (name key)} display-name])]]
        [:div.tincture
         [:label {:for "tincture"} "Tincture"]
         [:select {:name "tincture"
                   :id "tincture"
                   :value (name @(rf/subscribe [:get-in (concat path [:content :tincture])]))
                   :on-change #(rf/dispatch [:set-in (concat path [:content :tincture]) (keyword (-> % .-target .-value))])}
          (for [[group-name & options] tincture/options]
            ^{:key group-name}
            [:optgroup {:label group-name}
             (for [[display-name key] options]
               ^{:key key}
               [:option {:value (name key)} display-name])])]])]
     #_[:fieldset
        [:label {:for "ordinary"} "Ordinary"]
        [:select {:name "ordinary"
                  :id "ordinary"
                  :value (name @(rf/subscribe [:get-in (concat path [:ordinaries 0 :type])]))
                  :on-change #(rf/dispatch [:set-in (concat path [:ordinaries 0 :type]) (keyword (-> % .-target .-value))])}
         (for [[key display-name] ordinary/options]
           ^{:key key}
           [:option {:value (name key)} display-name])]]
     #_[:fieldset
        [:label {:for "line2"} "Line"]
        [:select {:name "line2"
                  :id "line2"
                  :value (name @(rf/subscribe [:get-in (concat path [:ordinaries 0 :line :style])]))
                  :on-change #(rf/dispatch [:set-in (concat path [:ordinaries 0 :line :style]) (keyword (-> % .-target .-value))])}
         (for [[key display-name] line/options]
           ^{:key key}
           [:option {:value (name key)} display-name])]]]))

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
