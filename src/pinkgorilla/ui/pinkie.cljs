(ns pinkgorilla.ui.pinkie
  (:require
   [reagent.core :as r :refer [atom]]
   [reagent.impl.template]
 ;  [taoensso.timbre :refer-macros (info)]
   [clojure.walk :refer [prewalk]] ; cljs 1.10 still does not have walk fixed
  ; [pinkgorilla.ui.walk :refer [prewalk]] ; TODO: replace this as soon as 1.11 cljs is out.
   ))

(def custom-renderers (atom {}))

(defn renderer-list []
  (map #(assoc {} :k (first %) :r (pr-str (last %))) (seq @custom-renderers)))

(defn register-tag [k v]
  (swap! custom-renderers assoc k v)
  ; it would be ideal to let reagent deal with this, but the below line did not work.
  ;(gobj/set reagent.impl.template/tag-name-cache (name k) v)
  )

; mfikes approach would be great, but does not work
; https://github.com/reagent-project/reagent/issues/362

#_(defn register-tag2 [k v]
    (gobj/set reagent.impl.template/tag-name-cache k v))

#_(defn register-tag3 [kw c]
    (register-tag2 (name kw) (r/as-element c)))

(defn clj->json
  [ds]
  (.stringify js/JSON (clj->js ds)))

(def html5-tags
  #{:<>   ; this is technically the reagent-ignore keyword
    :> ; another reagent tag
    :a :abbr :address :area :article :aside :audio
    :b :base :bdi :bdo :blockquote :body :br :button
    :canvas :caption :cite :code :col :colgroup
    :data :datalist :dd :del :dfn :div :dl :dt
    :em :embed
    :fieldset :figcaption :figure :footer :form
    :h1 :h2 :h3 :h4 :h5 :h6 :head :header :hr :html
    :i :iframe :img :input :ins
    :kbd :keygen
    :label :legend :li :link
    :main :map :mark :meta :meter
    :nav :noscript
    :object :ol :optgroup :option :output
    :p :param :pre :progress
    :q
    :rb :rp :rt :rtc :ruby
    :s :samp :script :section :select :small :source :span :strong :style :sub :sup
    :table :tbody :td :template :textarea :tfoot :th :thead :time :title :tr :track
    :u :ul
    :var :video
    :wbr})

(defn html5-tag? [tag]
  (let [; reagent also has :div#main.big which we have to transform to :div
        tag-typed (reagent.impl.template/cached-parse tag) ; #js {:name "<>", :id nil, :class nil, :custom false}
        ;_ (.log js/console "tag typed:" (pr-str tag-typed))
        tag-clean (keyword (:name (js->clj tag-typed :keywordize-keys true)))
        ;_ (.log js/console "tag clean:" tag-clean)
        ]
    (contains? html5-tags tag-clean)))

(def pinkie-namespace (namespace :p/test))

(defn pinkie-tag? [tag]
  (let [kw-namespace (namespace tag)]
    (= pinkie-namespace kw-namespace)))

(defn pinkie-exclude? [hiccup-vector]
  (contains? (meta hiccup-vector) :r))

(defn- hiccup-vector? [hiccup-vector]
  (and
   (vector? hiccup-vector)
   (not (map-entry? hiccup-vector)); ignore maps
   (keyword? (first hiccup-vector)); reagent syntax requires first element  to be a keyword
   ))

(defn should-replace? [hiccup-vector]
  (if (hiccup-vector? hiccup-vector)
    (let [tag (first hiccup-vector)]
      (and (not (pinkie-exclude? hiccup-vector))
           (not (html5-tag? tag))
           (pinkie-tag? tag)))
    false))

(defn unknown-tag
  "ui component for unknown tags - so that we don't need to catch react errors
   Currently not yet used (see resolve function)"
  [tag]
  [:div.unknown-tag {:style {:background-color "red"}}
   (str "Unknown Tag: " tag)])

(defn replace-tag-in-hiccup-vector
  "input: hiccup vector
   if keyword (first position in vector) has been registered via register-tag,
   then it gets replaced with the react function,
   otherwise keyword remains"
  [hiccup-vector]
  (let [_ (.log js/console "pinkie replacing: " (pr-str hiccup-vector))
        tag (first hiccup-vector)
        render-function (tag @custom-renderers)]
    (if (nil? render-function)
      (do (.log js/console "pinkie unknown tag: " (name tag))
          (unknown-tag tag))
      (into [] (assoc hiccup-vector 0 render-function)))))

(defn  tag-inject
  "resolve function-as symbol to function references in the reagent-hickup-map.
   Leaves regular hiccup data unchanged."
  [hiccup-vector]
  (prewalk
   (fn [x]
     (if (should-replace? x)
       (replace-tag-in-hiccup-vector x)
       x))
   hiccup-vector))


