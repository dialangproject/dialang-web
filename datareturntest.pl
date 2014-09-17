#!/usr/bin/perl -w

use strict;

use LWP::UserAgent;
use Digest::SHA qw(hmac_sha1_base64);
use Getopt::Long;

my $host = undef;
my $consumer_key = undef;
my $secret = undef;
my $user_id = undef;

GetOptions("host=s" => \$host,"key=s" => \$consumer_key,"secret=s" => \$secret,"userid=s" => \$user_id);

my $ua = LWP::UserAgent->new;
$ua->timeout(10);

my $launch_url = "http://$host/data";

# Hash it. The equals sign was crucial.
my $hash = hmac_sha1_base64($consumer_key, $secret) . '=';

my $response = $ua->post($launch_url,{'hash' => $hash,'consumer_key' => $consumer_key,'user_id' => $user_id});

if ($response->is_success) {
    print $response->content;  # or whatever
} else {
    die $response->status_line;
}
