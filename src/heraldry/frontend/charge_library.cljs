(ns heraldry.frontend.charge-library
  (:require ["svgo-browser/lib/get-svgo-instance" :as getSvgoInstance]
            [cljs.core.async :refer [go]]
            [cljs.core.async.interop :refer-macros [<p!]]
            [clojure.set :as set]
            [clojure.string :as s]
            [com.wsscode.common.async-cljs :refer [<? go-catch]]
            [heraldry.api.request :as api-request]
            [heraldry.coat-of-arms.attributes :as attributes]
            [heraldry.coat-of-arms.render :as render]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.frontend.charge-map :as charge-map]
            [heraldry.frontend.context :as context]
            [heraldry.frontend.credits :as credits]
            [heraldry.frontend.form.component :as component]
            [heraldry.frontend.form.core :as form]
            [heraldry.frontend.http :as http]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.user :as user]
            [heraldry.frontend.util :as util]
            [heraldry.util :refer [id-for-url full-url-for-username]]
            [hickory.core :as hickory]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as reife]))

(def form-db-path
  [:charge-form])

(def list-db-path
  [:charge-list])

(defn find-colours [data]
  (->> data
       (tree-seq #(or (map? %)
                      (vector? %)
                      (seq? %)) seq)
       (filter #(and (vector? %)
                     (-> % count (= 2))
                     (#{:fill :stroke} (first %))))
       (map second)
       (filter #(and (string? %)
                     (not= % "none")))
       (map svg/normalize-colour)
       (map s/lower-case)
       set))

(defn fetch-charges-for-user [user-id]
  (go
    (try
      (let [user-data (user/data)]
        (-> (api-request/call :fetch-charges-for-user {:user-id user-id}
                              user-data)
            <?
            :charges))
      (catch :default e
        (println "fetch-charges-for-user error:" e)))))

(defn fetch-charges []
  (go
    (try
      (let [user-data (user/data)]
        (-> (api-request/call :fetch-charges {}
                              user-data)
            <?
            :charges))
      (catch :default e
        (println "fetch-charges error:" e)))))

(defn fetch-charge [charge-id version]
  (go
    (try
      (let [user-data (user/data)
            response  (<? (api-request/call :fetch-charge {:id      charge-id
                                                           :version version} user-data))
            edn-data  (<? (http/fetch (:edn-data-url response)))
            svg-data  (<? (http/fetch (:svg-data-url response)))]
        (-> response
            (assoc-in [:data :edn-data] edn-data)
            (assoc-in [:data :svg-data] svg-data)))
      (catch :default e
        (println "fetch-charge error:" e)))))

(defn optimize-svg [data]
  (go-catch
   (-> {:removeUnknownsAndDefaults false}
       clj->js
       getSvgoInstance
       (.optimize data)
       <p!
       (js->clj :keywordize-keys true)
       :data)))

;; views


(defn load-svg-file [db-path data]
  (go-catch
   (try
     (-> data
         optimize-svg
         <?
         hickory/parse-fragment
         first
         hickory/as-hiccup
         (as-> parsed
             (let [edn-data (-> parsed
                                (assoc 0 :g)
                                (assoc 1 {}))
                   width    (-> parsed
                                (get-in [1 :width])
                                js/parseFloat)
                   height   (-> parsed
                                (get-in [1 :height])
                                js/parseFloat)
                   colours  (into {}
                                  (map (fn [c]
                                         [c :keep]) (find-colours
                                                     edn-data)))]
               (let [existing-colours @(rf/subscribe [:get (conj db-path :colours)])
                     new-colours      (merge colours
                                             (select-keys existing-colours
                                                          (set/intersection
                                                           (-> colours
                                                               keys
                                                               set)
                                                           (-> existing-colours
                                                               keys
                                                               set))))]
                 (rf/dispatch [:set (conj db-path :colours) new-colours]))
               (rf/dispatch [:set (conj db-path :data) {:edn-data {:data   edn-data
                                                                   :width  width
                                                                   :height height}
                                                        :svg-data data}]))))
     (catch :default e
       (println "error:" e)))))

(defn preview []
  (let [{:keys [data type]
         :as   form-data}      @(rf/subscribe [:get form-db-path])
        {:keys [edn-data]}     data
        prepared-charge-data   (-> form-data
                                   (assoc :data edn-data)
                                   (assoc :username (:username (user/data))))
        db-path                [:example-coa :coat-of-arms]
        render-options         @(rf/subscribe [:get [:example-coa :render-options]])
        coat-of-arms           @(rf/subscribe [:get db-path])
        {:keys [result
                environment]}  (render/coat-of-arms
                                (-> coat-of-arms
                                    (assoc-in [:field :components 0 :type] type)
                                    (assoc-in [:field :components 0 :data] prepared-charge-data))
                                100
                                (merge
                                 context/default
                                 {:render-options render-options
                                  :db-path        db-path}))
        {:keys [width height]} environment]
    [:svg {:viewBox             (str "0 0 " (-> width (* 5) (+ 20)) " " (-> height (* 5) (+ 20) (+ 30)))
           :preserveAspectRatio "xMidYMid meet"
           :style               {:width "97%"}}
     [:g {:transform "translate(10,10) scale(5,5)"}
      result]]))

(defn upload-file [event db-path]
  (let [file (-> event .-target .-files (.item 0))]
    (when file
      (let [reader (js/FileReader.)]
        (set! (.-onloadend reader) (fn []
                                     (let [raw-data (.-result reader)]
                                       (load-svg-file db-path raw-data))))
        (.readAsText reader file)))))

(defn save-charge-clicked []
  (let [payload   @(rf/subscribe [:get form-db-path])
        user-data (user/data)]
    (rf/dispatch-sync [:clear-form-errors form-db-path])
    (rf/dispatch-sync [:clear-form-message form-db-path])
    (go
      (try
        (let [response  (<? (api-request/call :save-charge payload user-data))
              charge-id (-> response :charge-id)]
          (println "save charge response" response)
          (rf/dispatch-sync [:set (conj form-db-path :id) charge-id])
          (state/invalidate-cache form-db-path [charge-id nil])
          (state/invalidate-cache form-db-path [charge-id 0])
          (rf/dispatch-sync [:set list-db-path nil])
          (state/invalidate-cache list-db-path (:user-id user-data))
          (rf/dispatch-sync [:set-form-message form-db-path (str "Charge saved, new version: " (:version response))])
          (reife/push-state :edit-charge-by-id {:id (id-for-url charge-id)}))
        (catch :default e
          (println "save-form error:" e)
          (rf/dispatch [:set-form-error form-db-path (:message (ex-data e))]))))))

(defn charge-form []
  (let [error-message @(rf/subscribe [:get-form-error form-db-path])
        form-message  @(rf/subscribe [:get-form-message form-db-path])
        on-submit     (fn [event]
                        (.preventDefault event)
                        (.stopPropagation event)
                        (save-charge-clicked))
        logged-in?    (-> (user/data)
                          :logged-in?)]
    [:div.pure-g {:on-click #(do (rf/dispatch [:ui-component-deselect-all])
                                 (rf/dispatch [:ui-submenu-close-all])
                                 (.stopPropagation %))}
     [:div.pure-u-1-2 {:style {:position "fixed"}}
      [preview]]
     [:div.pure-u-1-2 {:style {:margin-left "50%"
                               :width       "45%"}}
      [component/form-attribution (conj form-db-path :attribution)]
      [:form.pure-form.pure-form-aligned
       {:style        {:display "inline-block"}
        :on-key-press (fn [event]
                        (when (-> event .-code (= "Enter"))
                          (on-submit event)))
        :on-submit    on-submit}
       (when form-message
         [:div.form-message form-message])
       (when error-message
         [:div.error-message error-message])
       [:fieldset
        [form/field (conj form-db-path :name)
         (fn [& {:keys [value on-change]}]
           [:div.pure-control-group
            [:label {:for   "name"
                     :style {:width "6em"}} "Name"]
            [:input {:id        "name"
                     :value     value
                     :on-change on-change
                     :type      "text"
                     :style     {:margin-right "0.5em"}}]
            [form/checkbox (conj form-db-path :is-public) "Make public"
             :style {:width "7em"}]])]
        [form/field (conj form-db-path :type)
         (fn [& {:keys [value on-change]}]
           [:div.pure-control-group
            [:label {:for   "type"
                     :style {:width "6em"}} "Charge type"]
            [:input {:id        "type"
                     :value     value
                     :on-change on-change
                     :type      "text"}]])]
        [form/select (conj form-db-path :attitude) "Attitude" attributes/attitude-choices
         :grouped? true
         :label-style {:width "6em"}]
        [form/select (conj form-db-path :facing) "Facing" attributes/facing-choices
         :grouped? true
         :label-style {:width "6em"}]
        [:div.pure-control-group
         [:h4 {:style {:margin-top    "1em"
                       :margin-bottom "0.5em"}} "Attributes"]
         (for [[group-name & attributes] attributes/attribute-choices]
           ^{:key group-name}
           [:div {:style {:display "inline-block"}}
            (for [[display-name key] attributes]
              ^{:key key}
              [form/checkbox (conj form-db-path :attributes key) display-name])])]
        [:div.pure-control-group
         [:h4 {:style {:margin-top    "1em"
                       :margin-bottom "0.5em"}} "Colours"]
         (let [colours-path (conj form-db-path :colours)
               colours      @(rf/subscribe [:get colours-path])]
           (for [[k _] (sort-by first colours)]
             ^{:key k}
             [form/select (conj colours-path k)
              [:div.colour-preview.tooltip {:style {:background-color k}}
               [:div.bottom {:style {:top "30px"}}
                [:h3 {:style {:text-align "center"}} k]
                [:i]]]
              attributes/tincture-modifier-for-charge-choices
              :grouped? true
              :label-style {:width        "1.5em"
                            :margin-right "0.5em"}
              :style {:display      "inline-block"
                      :padding-left "0"
                      :margin-right "0.5em"}]))
         [form/checkbox [:example-coa :render-options :preview-original?]
          "Preview original (don't replace colours)" :style {:margin-right "0.5em"
                                                             :width        "20em"}]]]
       [:div.pure-control-group {:style {:text-align "right"
                                         :margin-top "10px"}}
        [form/field-without-error (conj form-db-path :data)
         (fn [& _]
           [:label.pure-button {:for   "upload"
                                :style {:display "inline-block"
                                        :width   "auto"
                                        :float   "left"}} "Upload SVG"
            [:input {:type      "file"
                     :accept    "image/svg+xml"
                     :id        "upload"
                     :on-change #(upload-file % form-db-path)
                     :style     {:display "none"}}]])]
        [:button.pure-button.pure-button
         {:type     "button"
          :on-click (let [match  @(rf/subscribe [:get [:route-match]])
                          route  (-> match :data :name)
                          params (-> match :parameters :path)]
                      (cond
                        (= route :edit-charge-by-id) #(reife/push-state :view-charge-by-id params)
                        (= route :create-charge)     #(reife/push-state :charges params)
                        :else                        nil))
          :style    {:margin-right "5px"}}
         "View"]
        (let [disabled? (not logged-in?)]
          [:button.pure-button.pure-button-primary {:type  "submit"
                                                    :class (when disabled?
                                                             "disabled")}
           "Save"])]]
      [component/form-render-options [:example-coa :render-options]]
      [component/form-for-coat-of-arms [:example-coa :coat-of-arms]]]]))

(defn charge-properties [charge]
  [:div.properties {:style {:display        "inline-block"
                            :line-height    "1.5em"
                            :vertical-align "middle"
                            :white-space    "normal"}}
   (when-let [attitude (-> charge
                           :attitude
                           (#(when (not= % :none) %)))]
     [:div.tag.attitude (util/translate attitude)])
   (when-let [facing (-> charge
                         :facing
                         (#(when (-> % #{:none :to-dexter} not) %)))]
     [:div.tag.facing (util/translate facing)])
   (for [attribute (->> charge
                        :attributes
                        (filter second)
                        (map first)
                        sort)]
     ^{:key attribute}
     [:div.tag.attribute (util/translate attribute)])
   (for [modifier (->> charge
                       :colours
                       (map second)
                       (filter #(-> %
                                    #{:primary
                                      :keep
                                      :outline
                                      :eyes-and-teeth}
                                    not))
                       sort)]
     ^{:key modifier}
     [:div.tag.modifier (util/translate modifier)])])

(defn charge-display [charge-id version]
  (let [user-data            (user/data)
        [status charge-data] (state/async-fetch-data
                              form-db-path
                              [charge-id version]
                              #(fetch-charge charge-id version))
        charge-id            (-> charge-data
                                 :id
                                 id-for-url)]
    (when (= status :done)
      [:div.pure-g {:on-click #(do (rf/dispatch [:ui-component-deselect-all])
                                   (rf/dispatch [:ui-submenu-close-all])
                                   (.stopPropagation %))}
       [:div.pure-u-1-2 {:style {:position "fixed"}}
        [preview]]
       [:div.pure-u-1-2 {:style {:margin-left "50%"
                                 :width       "45%"}}
        [:div.credits
         [credits/for-charge charge-data]]
        [charge-properties charge-data]
        (when (= (:username charge-data)
                 (:username user-data))
          [:div.pure-control-group {:style {:text-align    "right"
                                            :margin-top    "10px"
                                            :margin-bottom "10px"}}
           [:button.pure-button.pure-button-primary {:type     "button"
                                                     :on-click #(do
                                                                  (rf/dispatch-sync [:clear-form-errors form-db-path])
                                                                  (rf/dispatch-sync [:clear-form-message form-db-path])
                                                                  (reife/push-state :edit-charge-by-id {:id charge-id}))}
            "Edit"]])
        [component/form-render-options [:example-coa :render-options]]]])))

(defn link-to-charge [charge & {:keys [type-prefix?]}]
  (let [charge-id (-> charge
                      :id
                      id-for-url)]
    [:a {:href     (reife/href :view-charge-by-id {:id charge-id})
         :on-click #(do
                      (rf/dispatch-sync [:clear-form-errors form-db-path])
                      (rf/dispatch-sync [:clear-form-message form-db-path]))}
     (when type-prefix?
       (str (-> charge :type name) ": "))
     (:name charge)]))

(defn list-charges-for-user [user-id]
  (let [[status charge-list] (state/async-fetch-data
                              list-db-path
                              user-id
                              #(fetch-charges-for-user user-id))]
    (when (= status :done)
      (if (empty? charge-list)
        (when user-id
          [:div "None"])
        [:ul.charge-list
         (doall
          (for [charge charge-list]
            ^{:key (:id charge)}
            [:li.charge {:style {:white-space "nowrap"}}
             [link-to-charge charge]
             [charge-properties charge]]))]))))

(defn create-charge [match]
  (rf/dispatch [:set [:route-match] match])
  (let [[status _charge-form-data] (state/async-fetch-data
                                    form-db-path
                                    :new
                                    #(go
                                       ;; TODO: make a default charge here?
                                       {}))]
    (when (= status :done)
      [charge-form])))

(defn edit-charge [charge-id version]
  (let [[status _charge-form-data] (state/async-fetch-data
                                    form-db-path
                                    [charge-id version]
                                    #(fetch-charge charge-id version))]
    (when (= status :done)
      [charge-form])))

(defn matches-word [data word]
  (cond
    (keyword? data) (-> data name s/lower-case (s/includes? word))
    (string? data)  (-> data s/lower-case (s/includes? word))
    (map? data)     (some (fn [[k v]]
                            (or (and (keyword? k)
                                     (matches-word k word)
                                     ;; this would be an attribute entry, the value
                                     ;; must be truthy as well
                                     v)
                                (matches-word v word))) data)))

(defn filter-charges [charges filter-string]
  (if (or (not filter-string)
          (-> filter-string s/trim count zero?))
    charges
    (let [words (-> filter-string
                    (s/split #" +")
                    (->> (map s/lower-case)))]
      (filterv (fn [charge]
                 (every? (fn [word]
                           (some (fn [attribute]
                                   (-> charge
                                       (get attribute)
                                       (matches-word word)))
                                 [:name :type :attitude :facing :attributes :colours :username]))
                         words))
               charges))))

(defn show-charge-tree [charges & {:keys [remove-empty-groups?]}]
  [:div.tree
   (let [filter-db-path       [:ui :charge-tree-filter]
         filter-string        @(rf/subscribe [:get filter-db-path])
         filtered-charges     (filter-charges charges filter-string)
         remove-empty-groups? (or remove-empty-groups?
                                  (not= charges filtered-charges))
         charge-map           (charge-map/build-charge-map
                               filtered-charges
                               :remove-empty-groups? remove-empty-groups?)]
     [:<>
      [component/search-field filter-db-path]
      (if (empty? filtered-charges)
        [:div "None"]
        [component/tree-for-charge-map-new charge-map [] nil nil
         {:render-variant (fn [node]
                            (let [charge   (-> node :data)
                                  username (-> charge :username)]
                              [:div {:style {:display        "inline-block"
                                             :white-space    "normal"
                                             :vertical-align "top"
                                             :line-height    "1.5em"}}
                               [:div {:style {:display        "inline-block"
                                              :vertical-align "top"}}
                                [link-to-charge (-> node :data)]
                                " by "
                                [:a {:href   (full-url-for-username username)
                                     :target "_blank"} username]]
                               [charge-properties charge]]))}])])])

(defn view-list-charges []
  (let [[status charges] (state/async-fetch-data
                          [:all-charges]
                          :all-charges
                          fetch-charges)]
    [:div {:style {:padding "15px"}}
     [:button.pure-button.pure-button-primary
      {:on-click #(do
                    (rf/dispatch-sync [:clear-form-errors form-db-path])
                    (rf/dispatch-sync [:clear-form-message form-db-path])
                    (reife/push-state :create-charge))}
      "Create"]

     [:h4 "Available Charges"]

     (if (= status :done)

       [show-charge-tree charges]
       [:div "loading..."])]))

(defn edit-charge-by-id [{:keys [parameters] :as match}]
  (rf/dispatch [:set [:route-match] match])
  (let [id        (-> parameters :path :id)
        version   (-> parameters :path :version)
        charge-id (str "charge:" id)]
    [edit-charge charge-id version]))

(defn view-charge-by-id [{:keys [parameters]}]
  (let [id        (-> parameters :path :id)
        version   (-> parameters :path :version)
        charge-id (str "charge:" id)]
    [charge-display charge-id version]))
