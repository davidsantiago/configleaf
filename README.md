# configleaf

## Persistent profiles in Leiningen 2.

Sometimes you need to write software that behaves differently depending on the context
it is being used in. For example, when I write a web service, I want the
servlet to open additional ports and output additional debugging information on errors
when I am working on developing it, but not when I push it into production. In short,
I want build profiles.

Configleaf originally added this functionality to Leiningen
1. Leiningen 2 now has a native profile capability that is even more powerful
than what Configleaf provided. Now with version 0.4.0, Configleaf for Leiningen 2 just
fills in the missing features in Leiningen 2's profile support:

* Persistent ("sticky") profiles, which can be set and remain in effect until unset.
* Profiles are extended into the project itself by having the final
  project map made available to the project's code in a configurable
  namespace. This project map is also available to built JARs.


## Usage

The first step is learning about
[Leiningen's profiles](https://github.com/technomancy/leiningen), in
the "Profiles" section. Configleaf only provides user interface to
turn profiles on and off, and to make them available to the code in
the project itself, so you need to understand how they work.

### Sticky profiles

The only user-operable way of controlling profiles in use is using
Leiningen's built-in `with-profile` task, which requires you to list
all the profiles you'd like to have in effect for the task that is
given as its argument. 

When configleaf is installed, you can get a list of the currently
active profiles by doing `lein profiles`:

```
David$ lein profiles
debug
default
offline
test
user

Current sticky profiles:
```

Here, Configleaf has modified the built in `profiles` task to also
print out the currently active sticky profiles. In this case, since
none have been set, there are no current sticky profiles. We can set a
few profiles using the `set-profile` task:

```
David$ lein set-profile test stage
Warning: Unknown profile :stage is being set active.
Current sticky profiles: #{:test :stage}
```

Now two profiles have been set to active, `:test` and
`:stage`. Configleaf has warned that it cannot find any profile called
`:stage`, as it isn't present in the project map. But it has still set
the profile to stick.

If we set `:stage` by mistake, we can unset that profile using the `unset-profile` command:

```
David$ lein unset-profile stage
Current sticky profiles: #{:test}
```

If we wish to remove all sticky profiles at once, we can simply call
`unset-profile` with the `--all` flag:

```
David$ lein unset-profile --all
All profiles unset.
David$ lein profiles
debug
default
offline
test
user

Current sticky profiles:
```

Note that none of this is modifying the project map. It is simply
stashing a bit of state that will be automatically applied when you
run Leiningen tasks. This state is stored in the file
`.configleaf/current` in your project root. This file can be deleted
at any time with no ill effect (other than unsticking all the profiles
that were set.

Finally, note that the built-in `with-profile` task still works the
same as it ever did. Any profiles it sets are added to the set of
sticky profiles already in effect, as sticky profiles are as if there
is an implicit `with-profile` with those tasks.

### Project map access

The second half of Configleaf is making the project map, with all
active profiles merged in, available to the project itself, in
addition to within leiningen's own process. It does this in part by
outputting a Clojure source file into a location of your choice (by
default `{first of :source-paths project key}/cfg/current.clj`) that
contains the project map. Any code that is interested in the project
map, whether it is running in Leiningen, in your project, or in a JAR
built by Leiningen with Configleaf installed, can access it by loading
that namespace and accessing the `project` variable in it.

As an example, suppose you run the `repl` or `swank` task:

```
David$ lein swank
Connection opened on localhost port 4005.
```

Then within the SLIME repl, you can do the following:

```
user> (use 'cfg.current)
nil
user> (prn project)
{:compile-path "/Users/David/Documents/Development/Clojure/configleaf/target/classes", :group "configleaf", ...}
nil
```

So we can see that we were able to use the `cfg.current` namespace and
find the entire project map in the `project` var from that
namespace. As you would expect, you can now write code that `require`s
the `cfg.current` namespace and changes its behavior based on its
contents. This code will continue to work even when a JAR is built
from this project. The project map available in the JAR will be based
on the configurations that were included when the JAR was built
(either through sticky profiles or the `with-profile` task or none,
depending on which is the case).

## Configuration

You can change the behavior of Configleaf by setting some values in
the project map. Configleaf's configuration goes in the `:configleaf`
key, which should contain a map of option names to their values. Here are the current keys:

* `:config-source-path` - Set this key to the path to the source
  directory to output the project map namespace into. Note that this
  is the location of the source directory, not the full path of the
  file the namespae is in; that will be named automatically based on
  the namespace's name. By default, this value will be the first entry
  in the `:source-paths` key in the project.
* `:namespace` - Set this key to the name of the namespace, as a
  symbol, that you want the project map to be output to. By default,
  this has the value `'cfg.current`.
* `:verbose` - Set this key to true if you'd like Configleaf to print
  out which profiles are included whenever a task is run. By default,
  this key is false.

So for example, if the project map has the following map in the `:configleaf` key:

```clojure
:configleaf {:config-source-path "src/main/clojure"
             :namespace 'myproject.config
             :verbose true}
````

Then the project map will be at `myproject.config/project`, which is
in the file `src/main/clojure/myproject/config.clj`. When you run the
command: 

```
David$ lein with-profile test profiles
Performing task with-profile with profiles (:dev :user :default)
Performing task 'profiles' with profile(s): 'test'
Performing task profiles with profiles (:test)
debug
default
offline
test
user

Current sticky profiles:
```

Here you can see the verbose statements of Configleaf mixed in with
the statement output by the `with-profile` task. First the
`with-profile` task has its profiles output by Configleaf, before it
runs. Only the default Leiningen profiles are in effect when it
runs. Then it prints out its statement that it is running the
`profiles` task with the `test` profile. Then Configleaf prints out
the profiles in effect when the `profiles` task runs; just the `test`
profile. At the end, you can see that there were no sticky profiles in
effect. If we add the `prod` profile as a sticky profile:

```
David$ lein set-profile prod
Performing task set-profile with profiles (:dev :user :default)
Current sticky profiles: #{:prod}
David$ lein with-profile test profiles
Performing task with-profile with profiles (:dev :user :default)
Performing task 'profiles' with profile(s): 'test'
Performing task profiles with profiles (:test :prod)
debug
default
offline
test
user

Current sticky profiles: #{:prod}
```

Here we can see that the `prod` profile was added to the running of
the `profiles` task because it was set sticky, and the `test` profile
was added by the `with-profile` task.

Since all of this extra output is controled by the `:verbose` key in the `:configleaf` configuration map, you can actually make yourself a profile that has the `:verbose` key to true in a `:configleaf` map, and then set that profile to by sticky:

```clojure
;; In project.clj...
:profiles {:verbose-configleaf {:configleaf {:verbose true}}}
```

Then 

```
David$ lein set-profile verbose-configleaf
```

will make it so that you can switch Configleaf from verbose output to quiet output by setting or unsetting the `verbose-configleaf` profile.

```
David$ lein set-profile verbose-configleaf
Current sticky profiles: #{:prod :verbose-configleaf}
David$ lein jar
Performing task jar with profiles (:dev :user :default :prod :verbose-configleaf)
Created /Users/David/Documents/Development/Clojure/configleaf/target/configleaf-0.4.0.jar
```

## Installation

To install Configleaf, add the following to your project map as a plugin:

```
[configleaf "0.4.5"]
```
Then add the following key-value pair in the top level of your project map:

```
:hooks [configleaf.hooks]
``` 

That is all you need. But you will probably also want to add two
directories to your `.gitignore` file. The first is the directory
`.configleaf`, which will be in the same directory as your
project.clj. This directory holds the currently active profile. The
second is the namespace that is automatically generated by Configleaf
with your profile values. In most of the examples above, you'd want to
add "src/cfg/current.clj" or possibly "src/cfg" to your `.gitignore`, if there are no other files you will have in `src/cfg` that you wish to check into git.

## News

* Version 0.4.5
  * Bug fixes; remove one of the hooks which is no longer necessary in lein2. Don't use earlier 0.4
    series versions (harm is bounded to extra files being added to JARs or lein tasks failing).

* Version 0.4.3
  * Minor update to also add configleaf itself to dependencies, fixes similar bugs.

* Version 0.4.2
  * Minor update to automatically add leiningen-core to dependencies, fixes certain
    tasks were hooked but ran in project.

* Version 0.4.1
  * Minor update to ensure that project map metadata is baked along
    with the project map.

* Version 0.4.0
  * Extensive rewrite to work with Leiningen 2.

* Version 0.3.0
  * Renamed and reorganized the project map. Should be easier to explain and use now.

* Version 0.2.0
  * Addition of Java system properties to configurations.
  * Changes to configuration map format to allow system properties. 

## License

Copyright (C) 2011

Distributed under the Eclipse Public License, the same as Clojure.
