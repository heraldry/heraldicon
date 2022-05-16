(ns heraldicon.frontend.ui.element.charge-select
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.frontend.api.request :as api.request]
   [heraldicon.frontend.filter :as filter]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.state :as state]
   [heraldicon.frontend.user :as user]
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
