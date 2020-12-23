(ns or.coad.main
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require ["js-base64" :as base64]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [cljs.reader :as reader]
            [clojure.string :as s]
            [goog.string.format]  ;; required for release build
            [or.coad.blazon :as blazon]
            [or.coad.charge :as charge]
            [or.coad.division :as division]
            [or.coad.escutcheon :as escutcheon]
            [or.coad.field :as field]
            [or.coad.field-environment :as field-environment]
            [or.coad.filter :as filter]
            [or.coad.hatching :as hatching]
            [or.coad.line :as line]
            [or.coad.ordinary :as ordinary]
            [or.coad.tincture :as tincture]
            [or.coad.util :as util]
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
               (go
                 (->
                  (http/get name)
                  <!
                  :body
                  (as-> result
                        (let [parsed (if (string? result)
                                       (reader/read-string result)
                                       result)]
                          (rf/dispatch [:set :loaded-data name parsed])))))
               nil)))))

;; events


(def default-coat-of-arms
  {:escutcheon :heater
   :field {:content {:tincture :none}}})

(def default-ordinary
  {:type :pale
   :line {:style :straight}
   :field {:content {:tincture :none}}})

(def default-charge
  {:type :roundel
   :tincture {:primary :none}
   :variant :variant-roundel-1
   :hints {:outline? true}})

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
 :toggle-in
 (fn [db [_ path]]
   (update-in db path not)))

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
                                              (conj value)
                                              vec))))
(rf/reg-event-db
 :remove-ordinary
 (fn [db [_ path]]
   (let [ordinaries-path (drop-last path)
         index (last path)]
     (update-in db ordinaries-path (fn [ordinaries]
                                     (vec (concat (subvec ordinaries 0 index)
                                                  (subvec ordinaries (inc index)))))))))

(rf/reg-event-db
 :add-charge
 (fn [db [_ path charge]]
   (update-in db (conj path :charges) #(-> %
                                           (conj charge)
                                           vec))))
(rf/reg-event-db
 :remove-charge
 (fn [db [_ path]]
   (let [charge-path (drop-last path)
         index (last path)]
     (update-in db charge-path (fn [charges]
                                 (vec (concat (subvec charges 0 index)
                                              (subvec charges (inc index)))))))))

(rf/reg-event-db
 :update-charge
 (fn [db [_ path changes]]
   (update-in db path merge changes)))

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

(defn form-for-tincture [title path]
  [:div.tincture
   [:div.setting
    [:label {:for "tincture"} title]
    [:select {:name "tincture"
              :id "tincture"
              :value (name (or @(rf/subscribe [:get-in path]) :none))
              :on-change #(rf/dispatch [:set-in path (keyword (-> % .-target .-value))])}
     [:option {:value "none"} "None"]
     (for [[group-name & options] tincture/options]
       ^{:key group-name}
       [:optgroup {:label group-name}
        (for [[display-name key] options]
          ^{:key key}
          [:option {:value (name key)} display-name])])]]])

(defn form-for-ordinary [path]
  [:<>
   [:a.remove [:i.far.fa-trash-alt {:on-click #(rf/dispatch [:remove-ordinary path])}]]
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

(defn tree-for-charge-map [{:keys [type key name groups charges attitudes variants]}
                           tree-path
                           path selected-charge charge-variant-data & {:keys [charge-type
                                                                              charge-attitude]}]
  (-> [:ul]
      (into (for [[key group] (sort-by first groups)]
              (let [node-path (conj tree-path :groups key)
                    flag-path (-> path
                                  (concat [:hints :ui :charge-map])
                                  vec
                                  (conj node-path))
                    open? @(rf/subscribe [:get-in flag-path])]
                ^{:key key}
                [:li.group
                 [:span.node-name.clickable
                  {:on-click #(rf/dispatch [:toggle-in flag-path])}
                  (if open?
                    [:i.far.fa-minus-square]
                    [:i.far.fa-plus-square]) (:name group)]
                 (when open?
                   [tree-for-charge-map
                    group
                    node-path
                    path selected-charge charge-variant-data])])))
      (into (for [[key charge] (sort-by first charges)]
              (let [node-path (conj tree-path :charges key)
                    flag-path (-> path
                                  (concat [:hints :ui :charge-map])
                                  vec
                                  (conj node-path))
                    open? @(rf/subscribe [:get-in flag-path])]
                ^{:key key}
                [:li.charge
                 [:span.node-name.clickable
                  {:on-click #(rf/dispatch [:toggle-in flag-path])}
                  (if open?
                    [:i.far.fa-minus-square]
                    [:i.far.fa-plus-square]) [:b (:name charge)]]
                 (when open?
                   [tree-for-charge-map
                    charge
                    node-path
                    path selected-charge charge-variant-data
                    :charge-type (:key charge)])])))
      (into (for [[key attitude] (sort-by first attitudes)]
              (let [node-path (conj tree-path :attitudes key)
                    flag-path (-> path
                                  (concat [:hints :ui :charge-map])
                                  vec
                                  (conj node-path))
                    open? @(rf/subscribe [:get-in flag-path])]
                ^{:key key}
                [:li.attitude
                 [:span.node-name.clickable
                  {:on-click #(rf/dispatch [:toggle-in flag-path])}
                  (if open?
                    [:i.far.fa-minus-square]
                    [:i.far.fa-plus-square]) [:em (:name attitude)]]
                 (when open?
                   [tree-for-charge-map
                    attitude
                    node-path
                    path selected-charge charge-variant-data
                    :charge-type charge-type
                    :charge-attitude (:key attitude)])])))
      (into (for [[key variant] (sort-by first variants)]
              ^{:key key}
              [:li.variant
               [:span.node-name.clickable
                {:on-click #(rf/dispatch [:update-charge path {:type charge-type
                                                               :attitude charge-attitude
                                                               :variant (:key variant)}])}
                [:i.fa.fa-picture-o] (:name variant)]]))))

(defn form-for-charge [path]
  (let [charge @(rf/subscribe [:get-in path])
        charge-variant-data (charge/get-charge-variant-data charge)
        charge-map (charge/get-charge-map)
        supported-tinctures (:supported-tinctures charge-variant-data)
        sorted-supported-tinctures (filter supported-tinctures [:primary :armed :langued :attired :unguled])
        eyes-and-teeth-support (:eyes-and-teeth supported-tinctures)]
    (if (and charge-map
             charge-variant-data)
      [:<>
       [:a.remove [:i.far.fa-trash-alt {:on-click #(rf/dispatch [:remove-charge path])}]]
       [:div.charge
        [:div.title (s/join " " [(-> charge :type util/translate util/upper-case-first)
                                 (-> charge :attitude util/translate)])]
        [:div.tree
         [tree-for-charge-map charge-map [] path charge charge-variant-data]]
        [:div.placeholders
         [:div.title "Supported tinctures"]
         (for [t sorted-supported-tinctures]
           ^{:key t} [form-for-tincture
                      (util/upper-case-first (util/translate t))
                      (concat path [:tincture t])])
         (when eyes-and-teeth-support
           [:div.setting
            [:input {:type "checkbox"
                     :id "eyes-and-teeth"
                     :name "eyes-and-teeth"
                     :checked (-> charge
                                  :tincture
                                  :eyes-and-teeth
                                  boolean)
                     :on-change #(let [checked (-> % .-target .-checked)]
                                   (rf/dispatch [:set-in
                                                 (concat path [:tincture :eyes-and-teeth])
                                                 (if checked
                                                   :argent
                                                   nil)]))}]
            [:label {:for "eyes-and-teeth"} "White eyes and teeth"]])
         [:div.setting
          [:input {:type "checkbox"
                   :id "outline"
                   :name "outline"
                   :checked (-> charge
                                :hints
                                :outline?
                                boolean)
                   :on-change #(let [checked (-> % .-target .-checked)]
                                 (rf/dispatch [:set-in
                                               (concat path [:hints :outline?])
                                               checked]))}]
          [:label {:for "outline"} "Draw outline"]]]]]
      [:<>])))

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
                   [:i.far.fa-edit]]
                  [:span.same (str "Same as " (inc part))]]
                 [:<>
                  (when (>= idx mandatory-part-count)
                    [:a.remove {:on-click #(rf/dispatch [:set-in (concat path [:division :fields idx])
                                                         (mod idx mandatory-part-count)])}
                     [:i.far.fa-times-circle]])
                  [form-for-field (vec (concat path [:division :fields idx]))]])]))]])]
     (when (= division-type :none)
       [form-for-tincture "Tincture" (concat path [:content :tincture])])
     [:div.ordinaries-section
      [:div.title
       "Ordinaries"
       [:a.add {:on-click #(rf/dispatch [:add-ordinary path default-ordinary])} [:i.fas.fa-plus]]]
      [:div.ordinaries
       (let [ordinaries @(rf/subscribe [:get-in (conj path :ordinaries)])]
         (for [[idx _] (map-indexed vector ordinaries)]
           ^{:key idx}
           [form-for-ordinary (vec (concat path [:ordinaries idx]))]))]]
     [:div.charges-section
      [:div.title
       "Charges"
       [:a.add {:on-click #(rf/dispatch [:add-charge path default-charge])} [:i.fas.fa-plus]]]
      [:div.charges
       (let [charges @(rf/subscribe [:get-in (conj path :charges)])]
         (for [[idx _] (map-indexed vector charges)]
           ^{:key idx}
           [form-for-charge (vec (concat path [:charges idx]))]))]]]))

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
