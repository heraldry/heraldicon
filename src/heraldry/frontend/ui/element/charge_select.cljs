(ns heraldry.frontend.ui.element.charge-select
  (:require
   [cljs.core.async :refer [go]]
   [clojure.string :as s]
   [clojure.walk :as walk]
   [com.wsscode.common.async-cljs :refer [<?]]
   [heraldry.coat-of-arms.attributes :as attributes]
   [heraldry.frontend.api.request :as api-request]
   [heraldry.frontend.charge-map :as charge-map]
   [heraldry.frontend.filter :as filter]
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.macros :as macros]
   [heraldry.frontend.state :as state]
   [heraldry.frontend.ui.element.tags :as tags]
   [heraldry.frontend.user :as user]
   [heraldry.util :as util]
   [re-frame.core :as rf]
   [taoensso.timbre :as log]))

(def list-db-path
  [:charge-list])

(def node-icons
  {:group {:closed "fa-plus-square"
           :open "fa-minus-square"}
   :attitude {:closed "fa-plus-square"
              :open "fa-minus-square"}
   :facing {:closed "fa-plus-square"
            :open "fa-minus-square"}
   :charge {:closed "fa-plus-square"
            :open "fa-minus-square"}
   :variant {:normal "fa-image"}})

(def node-flag-db-path [:ui :charge-tree :nodes])

(defn fetch-charge [charge-id version target-path]
  (go
    (try
      (let [user-data (user/data)
            charge-data (<? (api-request/call :fetch-charge {:id charge-id
                                                             :version version} user-data))]
        (when target-path
          (rf/dispatch [:set target-path charge-data]))
        charge-data)
      (catch :default e
        (log/error "fetch charge error:" e)))))

(defn fetch-charge-list []
  (go
    (try
      (let [user-data (user/data)]
        (-> (api-request/call
             :fetch-charges-list
             {}
             user-data)
            <?
            :charges))
      (catch :default e
        (log/error "fetch charge list error:" e)))))

(defn tree-for-charge-map [{:keys [node-type name groups charges attitudes facings variants] :as node}
                           tree-path
                           selected-charge remaining-path-to-charge
                           {:keys [still-on-path? render-variant open-all?]
                            :as opts}]
  (let [flag-path (conj node-flag-db-path tree-path)
        flag-open? @(rf/subscribe [:get flag-path])
        open? (or (and open-all?
                       (nil? flag-open?))
                  (= node-type :_root)
                  (and (nil? flag-open?)
                       still-on-path?)
                  flag-open?)
        variant? (= node-type :variant)]
    (cond-> [:<>]
      variant? (conj
                [:div.node-name {:on-click nil
                                 :style {:color (when still-on-path? "#1b6690")
                                         :left 0}}
                 "\u2022 " [render-variant node]])
      (and (not variant?)
           (not= node-type
                 :_root)) (conj
                           [:div.node-name.clickable
                            {:on-click #(state/dispatch-on-event % [:set flag-path (not open?)])
                             :style {:color (when still-on-path? "#1b6690")}}
                            (if open?
                              [:i.far {:class (-> node-icons (get node-type) :open)}]
                              [:i.far {:class (-> node-icons (get node-type) :closed)}])
                            [:<>
                             [(cond
                                (and (= node-type :variant)
                                     still-on-path?) :b
                                (= node-type :charge) :b
                                (= node-type :attitude) :em
                                (= node-type :facing) :em
                                :else :<>) name]
                             (let [c (charge-map/count-variants node)]
                               (when (pos? c)
                                 [:span.count-badge c]))]])
      (and open?
           groups) (conj [:ul
                          (for [[key group] (sort-by first groups)]
                            (let [following-path? (and still-on-path?
                                                       (= (first remaining-path-to-charge)
                                                          key))
                                  remaining-path-to-charge (when following-path?
                                                             (drop 1 remaining-path-to-charge))]
                              ^{:key key}
                              [:li.group
                               [tree-for-charge-map
                                group
                                (conj tree-path :groups key)
                                selected-charge
                                remaining-path-to-charge
                                (-> opts
                                    (assoc :still-on-path? following-path?))]]))])
      (and open?
           charges) (conj [:ul
                           (for [[key charge] (sort-by first charges)]
                             (let [following-path? (and still-on-path?
                                                        (-> remaining-path-to-charge
                                                            count zero?)
                                                        (= (:type charge)
                                                           (:type selected-charge)))]
                               ^{:key key}
                               [:li.charge
                                [tree-for-charge-map
                                 charge
                                 (conj tree-path :charges key)
                                 selected-charge
                                 remaining-path-to-charge
                                 (-> opts
                                     (assoc :still-on-path? following-path?))]]))])
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
                                   selected-charge
                                   remaining-path-to-charge
                                   (-> opts
                                       (assoc :still-on-path? following-path?))]]))])
      (and open?
           facings) (conj [:ul
                           (for [[key facing] (sort-by first facings)]
                             (let [following-path? (and still-on-path?
                                                        (-> remaining-path-to-charge
                                                            count zero?)
                                                        (= (:key facing)
                                                           (:facing selected-charge)))]
                               ^{:key key}
                               [:li.variant
                                [tree-for-charge-map
                                 facing
                                 (conj tree-path :facings key)
                                 selected-charge
                                 remaining-path-to-charge
                                 (-> opts
                                     (assoc :still-on-path? following-path?))]]))])
      (and open?
           variants) (conj [:ul
                            (for [[key variant] (sort-by (comp :name second) variants)]
                              (let [following-path? (and still-on-path?
                                                         (-> remaining-path-to-charge
                                                             count zero?)
                                                         (= (:key variant)
                                                            (:facing selected-charge)))]
                                ^{:key key}
                                [:li.variant
                                 [tree-for-charge-map
                                  variant
                                  (conj tree-path :variants key)
                                  selected-charge
                                  remaining-path-to-charge
                                  (-> opts
                                      (assoc :still-on-path? following-path?))]]))]))))

(defn charge-properties [charge]
  [:div.properties {:style {:display "inline-block"
                            :line-height "1.5em"
                            :vertical-align "middle"
                            :white-space "normal"
                            :margin-left "0.5em"}}
   (when-let [attitude (-> charge
                           :attitude
                           (#(when (not= % :none) %)))]
     [:div.tag.attitude (util/translate attitude)])
   " "
   (when-let [facing (-> charge
                         :facing
                         (#(when (-> % #{:none :to-dexter} not) %)))]
     [:div.tag.facing (util/translate facing)])
   " "
   (for [attribute (->> charge
                        :attributes
                        (filter second)
                        (map first)
                        sort)]
     ^{:key attribute}
     [:<> [:div.tag.attribute (util/translate attribute)] " "])
   (when (or (->> charge :colours vals (map attributes/tincture-modifier) set :shadow)
             (->> charge :colours vals (map attributes/tincture-modifier-qualifier) (keep attributes/shadow-qualifiers) seq)
             (->> charge :colours vals (map attributes/tincture-modifier) set :highlight)
             (->> charge :colours vals (map attributes/tincture-modifier-qualifier) (keep attributes/highlight-qualifiers) seq))
     [:div.tag.shading "shading"])
   " "
   (when-let [fixed-tincture (-> charge
                                 :fixed-tincture
                                 (or :none)
                                 (#(when (not= % :none) %)))]
     [:div.tag.fixed-tincture (util/translate fixed-tincture)])
   " "
   (for [modifier (->> charge
                       :colours
                       (map second)
                       (keep attributes/tincture-modifier)
                       (filter #(-> %
                                    #{:primary
                                      :keep
                                      :outline
                                      :shadow
                                      :highlight}
                                    not))
                       set
                       sort)]
     ^{:key modifier}
     [:<> [:div.tag.modifier (util/translate modifier)] " "])
   [tags/tags-view (-> charge :tags keys)]])

(macros/reg-event-db :prune-false-flags
  (fn [db [_ path]]
    (update-in db path (fn [flags]
                         (walk/postwalk (fn [value]
                                          (if (map? value)
                                            (->> value
                                                 (filter #(-> % second (not= false)))
                                                 (into {}))
                                            value))
                                        flags)))))

(defn invalidate-charge-cache [key]
  (state/invalidate-cache list-db-path key))

(defn component [charge-list-path on-select refresh-fn & {:keys [hide-ownership-filter?
                                                                 selected-charge
                                                                 display-selected-item?]}]
  (let [user-data (user/data)]
    [filter/component
     :charge-list
     user-data
     charge-list-path
     [:name :type :attitude :facing :attributes :colours :username :metadata :tags]
     :charge
     on-select
     refresh-fn
     :sort-fn (juxt (comp s/lower-case :name)
                    :type
                    :id
                    :version)
     :page-size 20
     :hide-ownership-filter? hide-ownership-filter?
     :component-styles {:height "calc(80vh - 3em)"}
     :selected-item selected-charge
     :display-selected-item? display-selected-item?]))

(defn list-charges [on-select & {:keys [selected-charge
                                        display-selected-item?]}]
  (let [[status _charges-list] (state/async-fetch-data
                                list-db-path
                                :all
                                fetch-charge-list)]
    (if (= status :done)
      [component
       list-db-path
       on-select
       #(invalidate-charge-cache :all)
       :selected-charge selected-charge
       :display-selected-item? display-selected-item?]
      [:div [tr :string.miscellaneous/loading]])))
