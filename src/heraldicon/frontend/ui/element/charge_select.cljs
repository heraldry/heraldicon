(ns heraldicon.frontend.ui.element.charge-select
  (:require
   [cljs.core.async :refer [go]]
   [clojure.walk :as walk]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.blazonry :as blazonry]
   [heraldicon.frontend.api.request :as api.request]
   [heraldicon.frontend.filter :as filter]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.frontend.state :as state]
   [heraldicon.frontend.ui.element.tags :as tags]
   [heraldicon.frontend.user :as user]
   [heraldicon.heraldry.option.attributes :as attributes]
   [re-frame.core :as rf]
   [taoensso.timbre :as log]))

(def list-db-path
  [:charge-list])

(defn fetch-charge [charge-id version target-path]
  (go
    (try
      (let [user-data (user/data)
            charge-data (<? (api.request/call :fetch-charge {:id charge-id
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
        (-> (api.request/call
             :fetch-charges-list
             {}
             user-data)
            <?
            :charges))
      (catch :default e
        (log/error "fetch charge list error:" e)))))

(defn charge-properties [charge]
  [:div.properties {:style {:display "inline-block"
                            :line-height "1.5em"
                            :vertical-align "middle"
                            :white-space "normal"
                            :margin-left "0.5em"}}
   (when-let [attitude (-> charge
                           :attitude
                           (#(when (not= % :none) %)))]
     [:div.tag.attitude (blazonry/translate attitude)])
   " "
   (when-let [facing (-> charge
                         :facing
                         (#(when (-> % #{:none :to-dexter} not) %)))]
     [:div.tag.facing (blazonry/translate facing)])
   " "
   (for [attribute (->> charge
                        :attributes
                        (filter second)
                        (map first)
                        sort)]
     ^{:key attribute}
     [:<> [:div.tag.attribute (blazonry/translate attribute)] " "])
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
     [:div.tag.fixed-tincture (blazonry/translate fixed-tincture)])
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
     [:<> [:div.tag.modifier (blazonry/translate modifier)] " "])
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
     :sort-fn (juxt (comp filter/normalize-string-for-sort :name)
                    :type
                    :id
                    :version)
     :page-size 20
     :hide-ownership-filter? hide-ownership-filter?
     :component-styles (if display-selected-item?
                         {:height "75vh"}
                         {:height "90vh"})
     :selected-item selected-charge
     :display-selected-item? display-selected-item?]))

(defn list-charges [on-select & {:keys [selected-charge
                                        display-selected-item?]}]
  (let [[status _charges-list] (state/async-fetch-data
                                list-db-path
                                :all
                                fetch-charge-list
                                :on-success #(rf/dispatch
                                              [:heraldicon.frontend.ui.element.blazonry-editor/update-parser %]))]
    (if (= status :done)
      [component
       list-db-path
       on-select
       #(invalidate-charge-cache :all)
       :selected-charge selected-charge
       :display-selected-item? display-selected-item?]
      [:div [tr :string.miscellaneous/loading]])))
