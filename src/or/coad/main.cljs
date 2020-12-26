(ns or.coad.main
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require ["js-base64" :as base64]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [cljs.reader :as reader]
            [clojure.walk :as walk]
            [goog.string.format]  ;; required for release build
            [or.coad.blazon :as blazon]
            [or.coad.division :as division]
            [or.coad.escutcheon :as escutcheon]
            [or.coad.field :as field]
            [or.coad.field-environment :as field-environment]
            [or.coad.filter :as filter]
            [or.coad.form :as form]
            [or.coad.hatching :as hatching]
            [or.coad.ordinary :as ordinary]
            [or.coad.tincture :as tincture]
            [re-frame.core :as rf]
            [reagent.dom :as r]))

;; helper

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

(rf/reg-event-db
 :initialize-db
 (fn [db [_]]
   (merge {:options      {:mode      :colours
                          :outline?  false
                          :squiggly? false}
           :coat-of-arms form/default-coat-of-arms} db)))

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
 :set-division-type
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
                            default                      (division/default-fields new-type)]
                        (cond
                          (< (count current) (count default)) (into current (subvec default (count current)))
                          (> (count current) (count default)) (subvec current 0 (count default))
                          :else                               current))))
         (update-in (conj path :division :hints) (fn [{:keys [diagonal-mode] :as hints}]
                                                   (if (-> new-type
                                                           division/diagonal-options
                                                           (->> (map second))
                                                           set
                                                           (get diagonal-mode)
                                                           not)
                                                     (dissoc hints :diagonal-mode)
                                                     hints)))
         (update-in path dissoc :content)
         (cond->
             (not (division/counterchangable? new-type)) (update-in (conj path :ordinaries) (fn [ordinaries]
                                                                                              (->> ordinaries
                                                                                                   (map #(update % :field dissoc :counterchanged?))
                                                                                                   vec)))
             (not (division/counterchangable? new-type)) (update-in (conj path :charges) (fn [charges]
                                                                                           (->> charges
                                                                                                (map #(update % :field dissoc :counterchanged?))
                                                                                                vec))))))))

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
   (-> db
       (remove-key-recursively :selected?)
       (cond->
           path (assoc-in (conj path :ui :selected?) true)))))


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
    filter/glow
    tincture/patterns
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
             :fill   "#ddd"}]]
    (let [spacing 2
          width   (* spacing 2)
          size    0.3]
      [:pattern {:id            "selected"
                 :width         width
                 :height        width
                 :pattern-units "userSpaceOnUse"}
       [:rect {:x      0
               :y      0
               :width  width
               :height width
               :fill   "#f5f5f5"}]
       [:g {:fill "#000"}
        [:circle {:cx 0
                  :cy 0
                  :r  size}]
        [:circle {:cx width
                  :cy 0
                  :r  size}]
        [:circle {:cx 0
                  :cy width
                  :r  size}]
        [:circle {:cx width
                  :cy width
                  :r  size}]
        [:circle {:cx spacing
                  :cy spacing
                  :r  size}]]])]))

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

(defn forms []
  [:<>
   [form/form-general]
   [:div.title "Coat of Arms"]
   [form/form-for-field [:coat-of-arms :field]]])

(defn app []
  (fn []
    (let [coat-of-arms          @(rf/subscribe [:get :coat-of-arms])
          mode                  @(rf/subscribe [:get :options :mode])
          options               @(rf/subscribe [:get :options])
          stripped-coat-of-arms (remove-key-recursively coat-of-arms :ui)
          state-base64          (.encode base64 (prn-str stripped-coat-of-arms))]
      (when coat-of-arms
        (js/history.replaceState nil nil (str "#" state-base64)))
      [:<>
       [:div {:style    {:width    "100%"
                         :height   "100vh"
                         :position "relative"}
              :on-click #(rf/dispatch [:select-component nil])}
        [:svg {:id                  "svg"
               :style               {:width    "25em"
                                     :height   "32em"
                                     :position "absolute"
                                     :left     0
                                     :top      0}
               :viewBox             "0 0 520 1000"
               :preserveAspectRatio "xMidYMin slice"}
         defs
         (when (= mode :hatching)
           [:defs
            hatching/patterns])
         [render-shield coat-of-arms options :db-path [:coat-of-arms]]]
        [:div.blazonry {:style {:position "absolute"
                                :left     10
                                :top      "32em"
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
         [forms]]]
       [:div.credits
        [:a {:href   "https://github.com/or/coad/"
             :target "_blank"} "Code and resource attribution on " [:i.fab.fa-github] " github:or/coad"]]])))

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
