(ns or.coad.main
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require ["js-base64" :as base64]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [cljs.reader :as reader]
            [clojure.string :as s]
            [clojure.walk :as walk]
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

(defn remove-key-recursively [data key]
  (walk/postwalk #(cond-> %
                    (map? %) (dissoc key))
                 data))

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
   (let [division (get-in db (conj path :division :type))]
     (or division :none))))

(rf/reg-sub
 :load-data
 (fn [db [_ name]]
   (let [data (get-in db [:loaded-data name])]
     (cond
       (= data :loading) nil
       data              data
       :else             (do
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
   :field      {:content {:tincture :none}}})

(def default-ordinary
  {:type  :pale
   :line  {:style :straight}
   :field {:content {:tincture :none}}})

(def default-charge
  {:type     :roundel
   :tincture {:primary :none}
   :variant  :default
   :hints    {:outline? true}})

(rf/reg-event-db
 :initialize-db
 (fn [db [_]]
   (merge {:rendering    {:mode    :colours
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
         (assoc-in (conj path :division :type) new-type)
         (update-in (conj path :division :line :style) #(or % :straight))
         (update-in (conj path :division :fields)
                    (fn [current-value]
                      (let [current                      (or current-value [])
                            current-type                 (get-in db (conj path :division :type))
                            current-mandatory-part-count (division/mandatory-part-count current-type)
                            new-mandatory-part-count     (division/mandatory-part-count new-type)
                            min-mandatory-part-count     (min current-mandatory-part-count
                                                              new-mandatory-part-count)
                            current                      (if (or (= current-mandatory-part-count
                                                                    new-mandatory-part-count)
                                                                 (<= (count current) min-mandatory-part-count))
                                                           current
                                                           (subvec current 0 min-mandatory-part-count))
                            default                      (into [{:content {:tincture :none}}
                                                                {:content {:tincture :azure}}]
                                                               (cond
                                                                 (#{:per-saltire :quarterly} new-type)      [0 1]
                                                                 (= :gyronny new-type)                      [0 1 0 1 0 1]
                                                                 (#{:tierced-per-pale
                                                                    :tierced-per-fess
                                                                    :tierced-per-pairle
                                                                    :tierced-per-pairle-reversed} new-type) [{:content {:tincture :gules}}]))]
                        (cond
                          (< (count current) (count default)) (into current (subvec default (count current)))
                          (> (count current) (count default)) (subvec current 0 (count default))
                          :else                               current))))
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
         index           (last path)]
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
         index       (last path)]
     (update-in db charge-path (fn [charges]
                                 (vec (concat (subvec charges 0 index)
                                              (subvec charges (inc index)))))))))

(rf/reg-event-db
 :update-charge
 (fn [db [_ path changes]]
   (update-in db path merge changes)))

(rf/reg-event-db
 :select-component
 (fn [db [_ path]]
   (println "select" path)
   (-> db
       (remove-key-recursively :selected?)
       (assoc-in (conj path :ui :selected?) true))))


;; views


(def -current-id
  (atom 0))

(defn id [prefix]
  (str prefix "_" (swap! -current-id inc)))

(def defs
  (into
   [:defs
    filter/shadow
    filter/shiny
    [:pattern#void {:width         20
                    :height        20
                    :pattern-units "userSpaceOnUse"}
     [:rect {:x      0
             :y      0
             :width  20
             :height 20
             :fill   "#fff"}]
     [:rect {:x      0
             :y      0
             :width  10
             :height 10
             :fill   "#ddd"}]
     [:rect {:x      10
             :y      10
             :width  10
             :height 10
             :fill   "#ddd"}]]]))

(defn render-coat-of-arms [data]
  (let [division   (:division data)
        ordinaries (:ordinaries data)]
    [:<>
     [division/render division]
     (for [[idx ordinary] (map-indexed vector ordinaries)]
       ^{:key idx} [ordinary/render ordinary])]))

(defn render-shield [coat-of-arms options & {:keys [db-path]}]
  (let [shield      (escutcheon/field (:escutcheon coat-of-arms))
        environment (field-environment/transform-to-width shield 100)
        field       (:field coat-of-arms)]
    [:g {:filter "url(#shadow)"}
     [:g {:transform "translate(10,10) scale(5,5)"}
      [:defs
       [:clipPath#mask-shield
        [:path {:d      (:shape environment)
                :fill   "#fff"
                :stroke "none"}]]]
      [:g {:clip-path "url(#mask-shield)"}
       [:path {:d    (:shape environment)
               :fill "#f0f0f0"}]
       [field/render field environment options :db-path (conj db-path :field)]]
      (when (:outline? options)
        [:path.outline {:d (:shape environment)}])]]))

(declare form-for-field)

(defn form-for-tincture [title path]
  [:div.tincture
   (let [element-id (id "tincture")]
     [:div.setting
      [:label {:for element-id} title]
      [:select {:name      "tincture"
                :id        element-id
                :value     (name (or @(rf/subscribe [:get-in path]) :none))
                :on-change #(rf/dispatch [:set-in path (keyword (-> % .-target .-value))])}
       [:option {:value "none"} "None"]
       (for [[group-name & options] tincture/options]
         ^{:key group-name}
         [:optgroup {:label group-name}
          (for [[display-name key] options]
            ^{:key key}
            [:option {:value (name key)} display-name])])]])])

(defn form-for-ordinary [path]
  (let [selected? @(rf/subscribe [:get-in (conj path :ui :selected?)])]
    [:<>
     [:a.remove [:i.far.fa-trash-alt {:on-click #(rf/dispatch [:remove-ordinary path])}]]
     [:div.ordinary.component {:class (when selected? "selected")}
      (let [element-id (id "ordinary-type")]
        [:div.setting
         [:label {:for element-id} "Type"]
         [:select {:name      "ordinary-type"
                   :id        element-id
                   :value     (name @(rf/subscribe [:get-in (conj path :type)]))
                   :on-change #(rf/dispatch [:set-in (conj path :type) (keyword (-> % .-target .-value))])}
          (for [[key display-name] ordinary/options]
            ^{:key key}
            [:option {:value (name key)} display-name])]])
      (let [element-id      (id "line")
            line-style-path (conj path :line :style)]
        [:div.setting
         [:label {:for element-id} "Line"]
         [:select {:name      "line"
                   :id        element-id
                   :value     (name @(rf/subscribe [:get-in line-style-path]))
                   :on-change #(rf/dispatch [:set-in line-style-path (keyword (-> % .-target .-value))])}
          (for [[key display-name] line/options]
            ^{:key key}
            [:option {:value (name key)} display-name])]])
      [:div.parts
       [form-for-field (conj path :field)]]]]))

(def node-icons
  {:group    {:closed "fa-plus-square"
              :open   "fa-minus-square"}
   :attitude {:closed "fa-plus-square"
              :open   "fa-minus-square"}
   :charge   {:closed "fa-plus-square"
              :open   "fa-minus-square"}
   :variant  {:normal "fa-image"}})

(defn tree-for-charge-map [{:keys [key type name groups charges attitudes variants]}
                           tree-path db-path
                           selected-charge remaining-path-to-charge & {:keys [charge-type
                                                                              charge-attitude
                                                                              still-on-path?]}]

  (let [flag-path       (-> db-path
                            (concat [:ui :charge-map])
                            vec
                            (conj tree-path))
        db-open?        @(rf/subscribe [:get-in flag-path])
        open?           (or (= type :root)
                            (and (nil? db-open?)
                                 still-on-path?)
                            db-open?)
        charge-type     (if (= type :charge)
                          key
                          charge-type)
        charge-attitude (if (= type :attitude)
                          key
                          charge-attitude)]
    (cond-> [:<>]
      (not= type
            :root)    (conj
                       [:span.node-name.clickable
                        {:on-click (if (= type :variant)
                                     #(rf/dispatch [:update-charge db-path {:type     charge-type
                                                                            :attitude charge-attitude
                                                                            :variant  key}])
                                     #(rf/dispatch [:toggle-in flag-path]))
                         :style    {:color (when still-on-path? "#1b6690")}}
                        (if (= type :variant)
                          [:i.far {:class (-> node-icons (get type) :normal)}]
                          (if open?
                            [:i.far {:class (-> node-icons (get type) :open)}]
                            [:i.far {:class (-> node-icons (get type) :closed)}]))
                        [(cond
                           (and (= type :variant)
                                still-on-path?) :b
                           (= type :charge)     :b
                           (= type :attitude)   :em
                           :else                :<>) name]])
      (and open?
           groups)    (conj [:ul
                             (for [[key group] (sort-by first groups)]
                               (let [following-path?          (and still-on-path?
                                                                   (= (first remaining-path-to-charge)
                                                                      key))
                                     remaining-path-to-charge (when following-path?
                                                                (drop 1 remaining-path-to-charge))]
                                 ^{:key key}
                                 [:li.group
                                  [tree-for-charge-map
                                   group
                                   (conj tree-path :groups key)
                                   db-path selected-charge
                                   remaining-path-to-charge
                                   :charge-type charge-type
                                   :charge-attitude charge-attitude
                                   :still-on-path? following-path?]]))])
      (and open?
           charges)   (conj [:ul
                             (for [[key charge] (sort-by first charges)]
                               (let [following-path? (and still-on-path?
                                                          (-> remaining-path-to-charge
                                                              count zero?)
                                                          (= (:key charge)
                                                             (:type selected-charge)))]
                                 ^{:key key}
                                 [:li.charge
                                  [tree-for-charge-map
                                   charge
                                   (conj tree-path :charges key)
                                   db-path selected-charge
                                   remaining-path-to-charge
                                   :charge-type charge-type
                                   :charge-attitude charge-attitude
                                   :still-on-path? following-path?]]))])
      (and open?
           attitudes) (conj [:ul
                             (for [[key attitude] (sort-by first attitudes)]
                               (let [following-path? (and still-on-path?
                                                          (-> remaining-path-to-charge
                                                              count zero?)
                                                          (= (:key attitude)
                                                             (:attitude selected-charge)))]
                                 ^{:key key}
                                 [:li.attitude
                                  [tree-for-charge-map
                                   attitude
                                   (conj tree-path :attitudes key)
                                   db-path selected-charge
                                   remaining-path-to-charge
                                   :charge-type charge-type
                                   :charge-attitude charge-attitude
                                   :still-on-path? following-path?]]))])
      (and open?
           variants)  (conj [:ul
                             (for [[key variant] (sort-by first variants)]
                               (let [following-path? (and still-on-path?
                                                          (-> remaining-path-to-charge
                                                              count zero?)
                                                          (= (:key variant)
                                                             (:variant selected-charge)))]
                                 ^{:key key}
                                 [:li.variant
                                  [tree-for-charge-map
                                   variant
                                   (conj tree-path :variants key)
                                   db-path selected-charge
                                   remaining-path-to-charge
                                   :charge-type charge-type
                                   :charge-attitude charge-attitude
                                   :still-on-path? following-path?]]))]))))

(defn form-for-charge [path]
  (let [charge                     @(rf/subscribe [:get-in path])
        selected?                  @(rf/subscribe [:get-in (conj path :ui :selected?)])
        charge-variant-data        (charge/get-charge-variant-data charge)
        charge-map                 (charge/get-charge-map)
        supported-tinctures        (-> charge-variant-data
                                       :supported-tinctures
                                       set)
        sorted-supported-tinctures (filter supported-tinctures [:primary :armed :langued :attired :unguled])
        eyes-and-teeth-support     (:eyes-and-teeth supported-tinctures)]
    (if (and charge-map
             charge-variant-data)
      [:<>
       [:a.remove [:i.far.fa-trash-alt {:on-click #(rf/dispatch [:remove-charge path])}]]
       [:div.charge.component {:class (when selected? "selected")}
        [:div.title (s/join " " [(-> charge :type util/translate util/upper-case-first)
                                 (-> charge :attitude util/translate)])]
        [:div {:style {:margin-bottom "1em"}}
         [:div.placeholders
          {:style {:width "60%"
                   :float "left"}}
          (for [t sorted-supported-tinctures]
            ^{:key t} [form-for-tincture
                       (util/upper-case-first (util/translate t))
                       (conj path :tincture t)])]
         [:div
          {:style {:width "40%"
                   :float "left"}}
          (when eyes-and-teeth-support
            (let [element-id (id "eyes-and-teeth")]
              [:div.setting
               [:input {:type      "checkbox"
                        :id        element-id
                        :name      "eyes-and-teeth"
                        :checked   (-> charge
                                       :tincture
                                       :eyes-and-teeth
                                       boolean)
                        :on-change #(let [checked (-> % .-target .-checked)]
                                      (rf/dispatch [:set-in
                                                    (conj path :tincture :eyes-and-teeth)
                                                    (if checked
                                                      :argent
                                                      nil)]))}]
               [:label {:for element-id} "White eyes and teeth"]]))
          (let [element-id (id "outline")]
            [:div.setting
             [:input {:type      "checkbox"
                      :id        element-id
                      :name      "outline"
                      :checked   (-> charge
                                     :hints
                                     :outline?
                                     boolean)
                      :on-change #(let [checked (-> % .-target .-checked)]
                                    (rf/dispatch [:set-in
                                                  (conj path :hints :outline?)
                                                  checked]))}]
             [:label {:for element-id} "Draw outline"]])]
         [:div.spacer]]
        [:div.tree
         [tree-for-charge-map charge-map [] path charge
          (get-in charge-map
                  [:lookup (:type charge)])
          :still-on-path? true]]]]
      [:<>])))

(defn form-for-field [path]
  (let [division-type @(rf/subscribe [:get-division-type path])
        selected?     @(rf/subscribe [:get-in (conj path :ui :selected?)])]
    [:div.field.component {:class (when selected? "selected")}
     [:div.division
      (let [element-id (id "division-type")]
        [:div.setting
         [:label {:for element-id} "Division"]
         [:select {:name      "division-type"
                   :id        element-id
                   :value     (name division-type)
                   :on-change #(rf/dispatch [:set-division path (keyword (-> % .-target .-value))])}
          (for [[key display-name] (into [[:none "None"]] division/options)]
            ^{:key key}
            [:option {:value (name key)} display-name])]])
      (when (not= division-type :none)
        [:<>
         [:div.line
          (let [element-id (id "division-type")
                line-style (conj path :division :line :style)]
            [:div.setting
             [:label {:for element-id} "Line"]
             [:select {:name      "line"
                       :id        element-id
                       :value     (name @(rf/subscribe [:get-in line-style]))
                       :on-change #(rf/dispatch [:set-in line-style (keyword (-> % .-target .-value))])}
              (for [[key display-name] line/options]
                ^{:key key}
                [:option {:value (name key)} display-name])]])]
         [:div.title "Parts"]
         [:div.parts
          (let [content              @(rf/subscribe [:get-in (conj path :division :fields)])
                mandatory-part-count (division/mandatory-part-count division-type)]
            (for [[idx part] (map-indexed vector content)]
              (let [part-path (conj path :division :fields idx)]
                ^{:key idx}
                [:div.part
                 (if (number? part)
                   [:<>
                    [:a.change {:on-click #(rf/dispatch [:set-in part-path
                                                         (get content part)])}
                     [:i.far.fa-edit]]
                    [:span.same (str "Same as " (inc part))]]
                   [:<>
                    (when (>= idx mandatory-part-count)
                      [:a.remove {:on-click #(rf/dispatch [:set-in part-path
                                                           (mod idx mandatory-part-count)])}
                       [:i.far.fa-times-circle]])
                    [form-for-field part-path]])])))]])]
     (when (= division-type :none)
       [form-for-tincture "Tincture" (conj path :content :tincture)])
     [:div.ordinaries-section
      [:div.title
       "Ordinaries"
       [:a.add {:on-click #(rf/dispatch [:add-ordinary path default-ordinary])} [:i.fas.fa-plus]]]
      [:div.ordinaries
       (let [ordinaries @(rf/subscribe [:get-in (conj path :ordinaries)])]
         (for [[idx _] (map-indexed vector ordinaries)]
           ^{:key idx}
           [form-for-ordinary (conj path :ordinaries idx)]))]]
     [:div.charges-section
      [:div.title
       "Charges"
       [:a.add {:on-click #(rf/dispatch [:add-charge path default-charge])} [:i.fas.fa-plus]]]
      [:div.charges
       (let [charges @(rf/subscribe [:get-in (conj path :charges)])]
         (for [[idx _] (map-indexed vector charges)]
           ^{:key idx}
           [form-for-charge (conj path :charges idx)]))]]]))

(defn form-general []
  [:div.general.component
   [:div.title "General"]
   [:div.setting
    [:button {:on-click #(rf/dispatch-sync [:set :coat-of-arms default-coat-of-arms])}
     "Reset"]]
   (let [element-id (id "escutcheon")]
     [:div.setting
      [:label {:for element-id} "Escutcheon"]
      [:select {:name      "escutcheon"
                :id        element-id
                :value     (name @(rf/subscribe [:get :coat-of-arms :escutcheon]))
                :on-change #(rf/dispatch [:set :coat-of-arms :escutcheon (keyword (-> % .-target .-value))])}
       (for [[key display-name] escutcheon/options]
         ^{:key key}
         [:option {:value (name key)} display-name])]])
   (let [element-id (id "mode")]
     [:div.setting
      [:label {:for element-id} "Mode"]
      [:select {:name      "mode"
                :id        element-id
                :value     (name @(rf/subscribe [:get :rendering :mode]))
                :on-change #(let [new-mode (keyword (-> % .-target .-value))]
                              (rf/dispatch [:set :rendering :mode new-mode])
                              (case new-mode
                                :hatching (rf/dispatch [:set :rendering :outline :on])
                                :colours  (rf/dispatch [:set :rendering :outline :off])))}
       (for [[display-name key] [["Colours" :colours]
                                 ["Hatching" :hatching]]]
         ^{:key key}
         [:option {:value (name key)} display-name])]])
   (let [element-id (id "outline")]
     [:div.setting
      [:label {:for element-id} "Outline"]
      [:select {:name      "outline"
                :id        element-id
                :value     (name @(rf/subscribe [:get :rendering :outline]))
                :on-change #(rf/dispatch [:set :rendering :outline (keyword (-> % .-target .-value))])}
       (for [[display-name key] [["On" :on]
                                 ["Off" :off]]]
         ^{:key key}
         [:option {:value (name key)} display-name])]])])

(defn form []
  [:<>
   [form-general]
   [:div.title "Coat of Arms"]
   [form-for-field [:coat-of-arms :field]]])

(defn app []
  (fn []
    (let [coat-of-arms          @(rf/subscribe [:get :coat-of-arms])
          mode                  @(rf/subscribe [:get :rendering :mode])
          options               {:outline? (= @(rf/subscribe [:get :rendering :outline]) :on)
                                 :mode     mode}
          stripped-coat-of-arms (remove-key-recursively coat-of-arms :ui)
          state-base64          (.encode base64 (prn-str stripped-coat-of-arms))]
      (when coat-of-arms
        (js/history.replaceState nil nil (str "#" state-base64)))
      [:<>
       [:div {:style {:width    "100%"
                      :position "relative"}}
        [:svg {:id                  "svg"
               :style               {:width    "25em"
                                     :height   "30em"
                                     :position "absolute"
                                     :left     0
                                     :top      0}
               :viewBox             "0 0 520 1000"
               :preserveAspectRatio "xMidYMin slice"}
         defs
         (when (= mode :hatching)
           hatching/patterns)
         [render-shield coat-of-arms options :db-path [:coat-of-arms]]]
        [:div.blazonry {:style {:position "absolute"
                                :left     10
                                :top      "31em"
                                :width    "calc(25em - 20px)"
                                :padding  10
                                :border   "1px solid #ddd"}}
         [:span.disclaimer "Blazon (very rudimentary, very beta)"]
         [:div.blazon
          (blazon/encode-field (:field coat-of-arms) :root? true)]]
        [:div {:style {:position "absolute"
                       :left     "27em"
                       :top      0
                       :width    "calc(100vw - 27em)"
                       :height   "100vh"
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
